$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080'
$results = @()
function Check($name, $script) {
    try {
        & $script
        $script:results += [pscustomobject]@{ Name = $name; Pass = $true; Detail = '' }
        Write-Host "PASS  $name" -ForegroundColor Green
    } catch {
        $detail = $_.Exception.Message
        if ($_.ErrorDetails) { $detail = "$detail | $($_.ErrorDetails.Message)" }
        $script:results += [pscustomobject]@{ Name = $name; Pass = $false; Detail = $detail }
        Write-Host "FAIL  $name : $detail" -ForegroundColor Red
    }
}
function JsonBytes($obj) {
    Write-Output -NoEnumerate ([Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 8)))
}
function Headers($token) { @{ Authorization = "Bearer $token" } }
function Login($username, $password) {
    (Invoke-RestMethod -Uri "$base/api/admin/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes @{ username = $username; password = $password })).data
}
function CustomerLogin($username, $password) {
    (Invoke-RestMethod -Uri "$base/api/public/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes @{ username = $username; password = $password })).data
}

Check 'admin login without tenant code' {
    $script:admin = Login 'admin' '123456'
    if ($script:admin.user.roleCode -ne 'ADMIN') { throw 'role mismatch' }
    if ($script:admin.token.Length -lt 100) { throw 'token missing' }
    if ($script:admin.company.id -ne 1) { throw 'company missing' }
    if ([string]::IsNullOrWhiteSpace($script:admin.company.name)) { throw 'company name missing' }
    if ($script:admin.PSObject.Properties.Name -contains 'tenant') { throw 'tenant field still returned' }
}
Check 'platform endpoints removed' {
    try {
        Invoke-RestMethod -Uri "$base/api/platform/dashboard" -Headers (Headers $admin.token) | Out-Null
        throw 'platform endpoint still available'
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }
}
Check 'roles only admin and customer' {
    $roles = Invoke-RestMethod -Uri "$base/api/admin/users/roles" -Headers (Headers $admin.token)
    $codes = @($roles.data | ForEach-Object { $_.code })
    if ($codes -notcontains 'ADMIN' -or $codes -notcontains 'CUSTOMER') { throw 'core roles missing' }
    if ($codes -contains 'SALES') { throw 'sales role still exists' }
    if ($roles.data | Where-Object { $_.scope -eq 'TENANT' }) { throw 'tenant role scope still exists' }
}
Check 'admin dashboard' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/dashboard" -Headers (Headers $admin.token)
    if ($r.data.customers -lt 3) { throw 'customer count wrong' }
    if ($r.data.trend.Count -ne 6) { throw 'trend missing' }
}
Check 'admin customer list' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/customers?page=1&size=10" -Headers (Headers $admin.token)
    if ($r.data.total -lt 3) { throw 'customer list empty' }
}
Check 'admin quote list' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/quotes?page=1&size=10" -Headers (Headers $admin.token)
    if ($r.data.total -lt 3) { throw 'quote list empty' }
}
Check 'customer account create and login without tenant code' {
    $script:qaUsername = 'qa_customer_' + (Get-Random -Maximum 99999)
    $body = JsonBytes @{ username = $script:qaUsername; password = 'QaCustomer123'; name = '接口验收客户'; roleCode = 'CUSTOMER'; customerId = 1 }
    $created = Invoke-RestMethod -Uri "$base/api/admin/users" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body -Headers (Headers $admin.token)
    if ($created.code -ne 0) { throw 'qa customer create failed' }
    $script:qaUserId = $created.data.id
    $script:customer = CustomerLogin $script:qaUsername 'QaCustomer123'
    if ($script:customer.user.userType -ne 'CUSTOMER') { throw 'not customer' }
    if ($script:customer.customer.id -ne 1) { throw 'customer not bound' }
    if ($script:customer.PSObject.Properties.Name -contains 'tenant') { throw 'tenant field still returned' }
}
Check 'customer self registration without tenant code' {
    $suffix = '{0:D4}' -f (Get-Random -Maximum 9999)
    $body = JsonBytes @{
        contactName = '注册联系人'; phone = "1360000$suffix"
        username = "qa_reg_$suffix"; password = 'QaRegister123'
    }
    $resp = Invoke-WebRequest -Uri "$base/api/public/auth/register" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body
    $r = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
    if ($r.code -ne 0) { throw 'registration failed' }
    if ($r.data.user.userType -ne 'CUSTOMER') { throw 'registered user type wrong' }
    if ($r.data.customer.name -ne '注册联系人') { throw 'optional company name fallback wrong' }
    $script:regUserId = $r.data.user.id
    $script:regCustomerId = $r.data.customer.id
}
Check 'public portal home without tenant parameter' {
    $r = Invoke-RestMethod -Uri "$base/api/public/portal/home"
    if ($r.data.products.Count -lt 3) { throw 'products missing' }
    if ($r.data.menus.Count -lt 4) { throw 'menus missing' }
    if ($r.data.company.name -eq $null) { throw 'company missing' }
    $raw = $r | ConvertTo-Json -Depth 6
    if ($raw -match 'tenantId') { throw 'tenantId leaked' }
}
Check 'public product detail + masked certs' {
    $r = Invoke-RestMethod -Uri "$base/api/public/products/1"
    if ($r.data.certificates.Count -lt 2) { throw 'certs missing' }
    if ($r.data.certificates[0].certNo -notmatch '\*\*\*\*') { throw 'cert not masked' }
}
Check 'public qualification verify' {
    $r = Invoke-RestMethod -Uri "$base/api/public/verify?certNo=GL-HLJ-CERT-2026-0001&keyword=%E9%91%AB%E7%81%AB"
    if ($r.data.valid -ne $true) { throw 'verify invalid' }
    if ($r.data.enterprise -eq $null) { throw 'enterprise missing' }
    if (($r.data.PSObject.Properties.Name -contains 'customerName')) { throw 'privacy leak' }
}
Check 'public demand submit + track without tenant code' {
    $unique = '烟测客户' + (Get-Random -Maximum 99999)
    $body = JsonBytes @{
        customerName = $unique; contactName = '测试联系人'; phone = '13699998888'
        boilerType = 'steam'; fuelType = 'gas'; capacity = '4 t/h'; pressureLevel = 'low'
        usageDesc = '食品加工蒸汽'; budgetMin = 400000; budgetMax = 520000; region = '黑龙江哈尔滨'
        expectTime = '2027-03-31'; description = '接口验收提交'; source = 'H5'
    }
    $r = Invoke-RestMethod -Uri "$base/api/public/demands" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body -Headers @{ 'Idempotency-Key' = [guid]::NewGuid().ToString() }
    if ($r.data.demandNo -notmatch 'GL-HLJ-DEMAND') { throw 'demand no invalid' }
    $track = Invoke-RestMethod -Uri "$base/api/public/demands/track?demandNo=$($r.data.demandNo)&phone=13699998888"
    if ($track.data.status -ne 'NEW') { throw 'track failed' }
    $script:newDemandNo = $r.data.demandNo
}
Check 'duplicate demand blocked without force' {
    $body = JsonBytes @{
        customerName = '鑫火机械制造有限公司'; contactName = '张伟'; phone = '13800001111'
        boilerType = 'steam'; fuelType = 'gas'; capacity = '6 t/h'; pressureLevel = 'low'; source = 'H5'
    }
    try {
        Invoke-RestMethod -Uri "$base/api/public/demands" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body | Out-Null
        throw 'duplicate demand accepted'
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw }
    }
}
Check 'customer overview and own data' {
    $r = Invoke-RestMethod -Uri "$base/api/customer/overview" -Headers (Headers $customer.token)
    if ($r.data.customer.id -ne 1) { throw 'wrong customer' }
    if ($r.data.demandCount -lt 1) { throw 'demand missing' }
}
Check 'customer cannot read other customer demand' {
    try {
        Invoke-RestMethod -Uri "$base/api/customer/demands/2" -Headers (Headers $customer.token) | Out-Null
        throw 'customer saw other demand'
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 403 -and $_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }
}
Check 'quote share + customer quote list' {
    $q = Invoke-RestMethod -Uri "$base/api/public/share/quote?token=token-quote-1"
    if ($q.data.quote.quoteNo -ne 'HLJ-QUOTE-2026-0001') { throw 'quote share failed' }
    if ($q.PSObject.Properties.Name -contains 'tenant') { throw 'share tenant field still returned' }
    $mine = Invoke-RestMethod -Uri "$base/api/customer/quotes" -Headers (Headers $customer.token)
    if ($mine.data.Count -lt 1) { throw 'customer quotes empty' }
}
Check 'quote PDF download' {
    $path = Join-Path $env:TEMP 'xh-quote.pdf'
    Invoke-WebRequest -Uri "$base/api/admin/quotes/1/pdf" -Headers (Headers $admin.token) -OutFile $path
    if ((Get-Item $path).Length -lt 10000) { throw 'pdf too small' }
}
Check 'customer Excel export' {
    $path = Join-Path $env:TEMP 'xh-customers.xlsx'
    Invoke-WebRequest -Uri "$base/api/admin/customers/export" -Headers (Headers $admin.token) -OutFile $path
    if ((Get-Item $path).Length -lt 3000) { throw 'xlsx too small' }
}
Check 'storage config masked and local test' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/storage" -Headers (Headers $admin.token)
    if ($r.data.driver -ne 'LOCAL') { throw 'storage driver wrong' }
    $body = JsonBytes @{ driver = 'LOCAL'; config = @{ basePath = 'd:\ai\boiler-platform\uploads' } }
    $t = Invoke-RestMethod -Uri "$base/api/admin/storage/test" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body -Headers (Headers $admin.token)
    if ($t.data.success -ne $true) { throw 'storage test failed' }
}
Check 'wechat config secret masking' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/wechat/config" -Headers (Headers $admin.token)
    if ($r.data.oaToken -ne 'xinhuo-oa-token') { throw 'config missing' }
    if ($r.data.mpSecret -ne '') { throw 'unexpected secret' }
}
Check 'template message fallback log' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/notifications/logs?page=1&size=10" -Headers (Headers $admin.token)
    if ($r.data.total -lt 1) { throw 'notify logs missing' }
}
Check 'warnings aggregated' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/warnings" -Headers (Headers $admin.token)
    if ($r.data.total -lt 1) { throw 'warnings empty' }
}
Check 'MP tabbar menu public without tenant parameter' {
    $r = Invoke-RestMethod -Uri "$base/api/public/menus?target=MP"
    if ($r.data.Count -ne 4) { throw "expected 4 MP tabs, got $($r.data.Count)" }
}
Check 'company config and standard dicts' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/settings/configs" -Headers (Headers $admin.token)
    if ($r.data | Where-Object { $_.configKey -eq 'tenant_name' }) { throw 'tenant_name config still exists' }
    if (-not ($r.data | Where-Object { $_.configKey -eq 'company_name' })) { throw 'company_name config missing' }
    $d = Invoke-RestMethod -Uri "$base/api/admin/settings/dicts" -Headers (Headers $admin.token)
    if ($d.data.global.Count -lt 30) { throw 'global dicts missing' }
}
Check 'announcements use system/company sources' {
    $r = Invoke-RestMethod -Uri "$base/api/public/announcements"
    if ($r.data | Where-Object { $_.source -in @('PLATFORM', 'TENANT') }) { throw 'legacy announcement source still exists' }
    $companyCount = @($r.data | Where-Object { $_.source -eq 'COMPANY' }).Count
    if ($companyCount -lt 1) { throw 'company announcements missing' }
}
Check 'audit log recorded without tenant wording' {
    $r = Invoke-RestMethod -Uri "$base/api/admin/audits?page=1&size=50" -Headers (Headers $admin.token)
    if ($r.data.total -lt 1) { throw 'audit missing' }
    $raw = $r | ConvertTo-Json -Depth 6
    if ($raw -match '租户') { throw 'tenant wording still exists' }
}

if ($script:qaUserId) {
    try { Invoke-RestMethod -Uri "$base/api/admin/users/$($script:qaUserId)" -Method Delete -Headers (Headers $admin.token) | Out-Null } catch {}
}
if ($script:regUserId) {
    try { Invoke-RestMethod -Uri "$base/api/admin/users/$($script:regUserId)" -Method Delete -Headers (Headers $admin.token) | Out-Null } catch {}
}
if ($script:regCustomerId) {
    try { Invoke-RestMethod -Uri "$base/api/admin/customers/$($script:regCustomerId)" -Method Delete -Headers (Headers $admin.token) | Out-Null } catch {}
}

$passed = ($results | Where-Object Pass).Count
$failed = ($results | Where-Object { -not $_.Pass }).Count
Write-Host ""
Write-Host "RESULT: $passed passed, $failed failed, total $($results.Count)" -ForegroundColor Cyan
$results | Where-Object { -not $_.Pass } | Format-Table -AutoSize
if ($failed -gt 0) { exit 1 }
