$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080'
function JsonBytes($obj) { Write-Output -NoEnumerate ([Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 8))) }
function AdminLogin($username, $password) {
    (Invoke-RestMethod "$base/api/admin/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes @{username=$username;password=$password})).data
}
function AuthHeaders($token) { @{ Authorization = "Bearer $token" } }
function AdminPost($path, $obj, $token) {
    (Invoke-RestMethod "$base$path" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes $obj) -Headers (AuthHeaders $token)).data
}
function AdminGet($path, $token) {
    (Invoke-RestMethod "$base$path" -Headers (AuthHeaders $token)).data
}
function AdminDelete($path, $token) {
    Invoke-RestMethod "$base$path" -Method Delete -Headers (AuthHeaders $token) | Out-Null
}
function PublicPost($path, $obj) {
    (Invoke-RestMethod "$base$path" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes $obj)).data
}

$suffix = (Get-Random -Minimum 1000 -Maximum 9999)
$username = "qa_release_$suffix"
$password = 'Release123'
$contactName = 'Release QA'
$phone = "1390000$suffix"
$ids = @{}
$maxAuditId = 0
try {
    $maxAuditId = [int](& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --port=3307 -uroot -proot -N -e "USE xh_boiler; SELECT COALESCE(MAX(id),0) FROM audit_log;" 2>$null)
} catch {}
try {
$admin = AdminLogin 'admin' '123456'
Write-Host 'PASS admin login'

# 0. Public portal, cases, products, health
$homeData = Invoke-RestMethod "$base/api/public/portal/home"
if ($homeData.data.cases.Count -lt 5) { throw 'public cases missing' }
if ($homeData.data.products.Count -lt 1) { throw 'public products missing' }
Write-Host 'PASS public portal home'

# 1. Customer registration + unified login
$reg = PublicPost '/api/public/auth/register' @{ contactName=$contactName; phone=$phone; username=$username; password=$password; companyName='Release QA Co'; industry='Test'; region='Test' }
$ids.customer = [long]$reg.customer.id
$customerToken = $reg.token
if ($reg.user.userType -ne 'CUSTOMER') { throw 'registered user type wrong' }
Write-Host "PASS customer register customerId=$($ids.customer)"
$unified = PublicPost '/api/public/auth/user-login' @{ username=$username; password=$password }
if ($unified.user.userType -ne 'CUSTOMER') { throw 'unified customer login failed' }
Write-Host 'PASS unified customer login'

# 2. Customer profile + demand create/claim/follow/status
$profile = Invoke-RestMethod "$base/api/customer/profile" -Headers (AuthHeaders $customerToken)
$contactId = $profile.data.contacts[0].id
$demand = AdminPost '/api/admin/demands' @{ customerId=$ids.customer; contactId=$contactId; contactName=$contactName; contactPhoneMask=($phone.Substring(0,3) + '****' + $phone.Substring(7)); boilerType='steam'; fuelType='gas'; capacity='6 t/h'; pressureLevel='low'; budgetMin=300000; budgetMax=450000; region='Test'; usageDesc='Release flow test'; description='Release flow test demand'; source='MANUAL' } $admin.token
$ids.demand = [long]$demand.id
AdminPost "/api/admin/demands/$($ids.demand)/claim" @{} $admin.token | Out-Null
AdminPost "/api/admin/demands/$($ids.demand)/follow" @{ method='PHONE'; content='Release flow follow'; nextTime=(Get-Date).AddDays(2).ToString('yyyy-MM-dd HH:mm:ss') } $admin.token | Out-Null
foreach ($s in @('CONSULTING','INTRODUCING','TALKING')) { AdminPost "/api/admin/demands/$($ids.demand)/status" @{ status=$s; remark='release flow' } $admin.token | Out-Null }
Write-Host 'PASS demand claim/follow/status'

# 3. Solution create/send/public confirm
$rec = AdminGet "/api/admin/demands/recommend?demandId=$($ids.demand)" $admin.token
if ($rec.Count -eq 0) { $products = (AdminGet '/api/admin/products?page=1&size=50' $admin.token).records; $product = $products[0] } else { $product = $rec[0].product }
$ids.product = [long]$product.id
$solution = AdminPost '/api/admin/solutions' @{ demandId=$ids.demand; name='Release Flow Solution'; productId=$ids.product; content='<h2>Release flow</h2><p>Solution verification.</p>' } $admin.token
$ids.solution = [long]$solution.id
AdminPost "/api/admin/solutions/$($ids.solution)/send" @{} $admin.token | Out-Null
$sd = AdminGet "/api/admin/solutions/$($ids.solution)" $admin.token
Invoke-RestMethod "$base/api/public/share/solution/confirm?token=$($sd.solution.shareToken)" -Method Post | Out-Null
Write-Host 'PASS solution create/send/confirm'

# 4. Quote create/send/public confirm -> contract
$quote = AdminPost '/api/admin/quotes' @{ demandId=$ids.demand; customerId=$ids.customer; solutionId=$ids.solution; productId=$ids.product; productName=$product.model; unitPrice=340000; quantity=1; taxRate=0.13; freight=10000; installFee=20000; floorPrice=300000; paymentTerms='30/40/30'; warranty='12 months'; validUntil=(Get-Date).AddDays(30).ToString('yyyy-MM-dd') } $admin.token
$ids.quote = [long]$quote.id
AdminPost "/api/admin/quotes/$($ids.quote)/send" @{} $admin.token | Out-Null
$qd = AdminGet "/api/admin/quotes/$($ids.quote)" $admin.token
Invoke-RestMethod "$base/api/public/share/quote/confirm?token=$($qd.quote.shareToken)" -Method Post | Out-Null
$qd = AdminGet "/api/admin/quotes/$($ids.quote)" $admin.token
if ($qd.contracts.Count -lt 1) { throw 'contract not generated after quote confirm' }
$ids.contract = [long]$qd.contracts[0].id
Write-Host "PASS quote confirm -> contract $($qd.contracts[0].contractNo)"

# 5. Contract approve/sign/payment/acceptance/equipment
AdminPost "/api/admin/contracts/$($ids.contract)/approve" @{ approved=$true; comment='release flow approve' } $admin.token | Out-Null
$signed = AdminPost "/api/admin/contracts/$($ids.contract)/sign" @{ signType='ELECTRONIC' } $admin.token
$cd = AdminGet "/api/admin/contracts/$($ids.contract)" $admin.token
AdminPost "/api/admin/contracts/payment-plans/$($cd.payments[0].id)/receive" @{ amount=$cd.payments[0].amount } $admin.token | Out-Null
$acceptance = $cd.installations | Where-Object { $_.stage -eq 'ACCEPTANCE' } | Select-Object -First 1
AdminPost "/api/admin/contracts/$($ids.contract)/installations/$($acceptance.id)/update" @{ stage='ACCEPTANCE'; status='DONE'; planDate=$acceptance.planDate; actualDate=(Get-Date).ToString('yyyy-MM-dd'); acceptanceSigner='Release QA'; remark='release flow'; photos=''; acceptanceUrl='' } $admin.token | Out-Null
$after = AdminGet "/api/admin/contracts/$($ids.contract)" $admin.token
if ($after.equipment.Count -lt 1) { throw 'equipment not generated after acceptance' }
$ids.equipment = [long]$after.equipment[0].id
Write-Host "PASS contract sign/payment/acceptance/equipment $($after.equipment[0].equipmentNo)"

# 6. PDFs
foreach ($path in @("/api/admin/quotes/$($ids.quote)/pdf", "/api/admin/contracts/$($ids.contract)/pdf", "/api/admin/contracts/$($ids.contract)/acceptance-pdf", "/api/admin/contracts/$($ids.contract)/installation-pdf")) {
    $file = Join-Path $env:TEMP ((Split-Path $path -Leaf) + '-release.pdf')
    Invoke-WebRequest "$base$path" -Headers (AuthHeaders $admin.token) -OutFile $file
    if ((Get-Item $file).Length -lt 5000) { throw "pdf too small: $path" }
}
Write-Host 'PASS quote/contract/acceptance/installation PDF'

# 7. Customer ticket -> admin accept/complete/review
$ticket = Invoke-RestMethod "$base/api/customer/tickets" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes @{ equipmentId=$ids.equipment; type='REPAIR'; priority='NORMAL'; description='Release flow ticket'; contactName=$contactName }) -Headers (AuthHeaders $customerToken)
$ids.ticket = [long]$ticket.data.id
AdminPost "/api/admin/tickets/$($ids.ticket)/accept" @{} $admin.token | Out-Null
AdminPost "/api/admin/tickets/$($ids.ticket)/complete" @{ solution='Release flow repaired' } $admin.token | Out-Null
AdminPost "/api/admin/tickets/$($ids.ticket)/review" @{ score=5; comment='Release flow satisfied' } $admin.token | Out-Null
Write-Host 'PASS ticket create/accept/complete/review'

# 8. Reports/warnings/customer overview
$reports = AdminGet '/api/admin/reports' $admin.token
if (-not $reports.overview) { throw 'reports missing' }
$warnings = AdminGet '/api/admin/warnings' $admin.token
if (-not $warnings.total -and $warnings.total -ne 0) { throw 'warnings invalid' }
$overview = Invoke-RestMethod "$base/api/customer/overview" -Headers (AuthHeaders $customerToken)
if ($null -eq $overview.data) { throw 'customer overview missing' }
Write-Host 'PASS reports/warnings/customer overview'

Write-Host "RELEASE FLOW PASS quote=$($qd.quote.quoteNo) contract=$($qd.contracts[0].contractNo) equipment=$($after.equipment[0].equipmentNo)"
$success = $true
} finally {
    Write-Host "cleanup ids: $($ids | ConvertTo-Json -Compress)"
    try { if ($ids.ticket) { AdminDelete "/api/admin/tickets/$($ids.ticket)" $admin.token } } catch { Write-Host "cleanup ticket: $_" }
    try { if ($ids.contract) { AdminDelete "/api/admin/contracts/$($ids.contract)" $admin.token } } catch { Write-Host "cleanup contract: $_" }
    try { if ($ids.quote) { AdminDelete "/api/admin/quotes/$($ids.quote)" $admin.token } } catch { Write-Host "cleanup quote: $_" }
    try { if ($ids.solution) { AdminDelete "/api/admin/solutions/$($ids.solution)" $admin.token } } catch { Write-Host "cleanup solution: $_" }
    try { if ($ids.demand) { AdminDelete "/api/admin/demands/$($ids.demand)" $admin.token } } catch { Write-Host "cleanup demand: $_" }
    try { if ($ids.customer) { AdminDelete "/api/admin/customers/$($ids.customer)" $admin.token } } catch { Write-Host "cleanup customer: $_" }
}

