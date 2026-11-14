$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080'
function JsonBytes($obj) {
    $json = $obj | ConvertTo-Json -Depth 8
    Write-Output -NoEnumerate ([Text.Encoding]::UTF8.GetBytes($json))
}
function Login($username, $password) {
    $data = (Invoke-RestMethod "$base/api/admin/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes @{username=$username;password=$password})).data
    return $data
}
function AuthHeaders($token) { @{ Authorization = "Bearer $token" } }
function PostJson($path, $obj, $token) {
    (Invoke-RestMethod "$base$path" -Method Post -ContentType 'application/json; charset=utf-8' -Body (JsonBytes $obj) -Headers (AuthHeaders $token)).data
}
function GetJson($path, $token) {
    (Invoke-RestMethod "$base$path" -Headers (AuthHeaders $token)).data
}

$admin = Login 'admin' '123456'
Write-Host "admin=$($admin.user.roleCode)"

# 1. 待认领需求认领 + 跟进 + 状态推进
PostJson '/api/admin/demands/2/claim' @{} $admin.token | Out-Null
PostJson '/api/admin/demands/2/follow' @{ method='PHONE'; content='验证链路：客户确认生物质锅炉需求，约定现场勘查'; nextTime=(Get-Date).AddDays(2).ToString('yyyy-MM-dd HH:mm:ss') } $admin.token | Out-Null
foreach ($s in @('CONSULTING','INTRODUCING','TALKING')) {
    PostJson '/api/admin/demands/2/status' @{ status=$s; remark='链路验收' } $admin.token | Out-Null
}
Write-Host 'PASS demand claim/follow/status'

# 2. 选型推荐 + 方案 + 推送 + 门户确认
$rec = GetJson '/api/admin/demands/recommend?demandId=2' $admin.token
if (-not $rec -or $rec.Count -eq 0) { throw 'no recommendation' }
$product = $rec[0].product
$solution = PostJson '/api/admin/solutions' @{ demandId=2; name='需求链路验收方案'; productId=$product.id; content='<h2>链路验收方案</h2><p>配置锅炉及辅机，含安装与一年维保。</p>' } $admin.token
PostJson "/api/admin/solutions/$($solution.id)/send" @{} $admin.token | Out-Null
$solutionDetail = GetJson "/api/admin/solutions/$($solution.id)" $admin.token
$solutionToken = $solutionDetail.solution.shareToken
Invoke-RestMethod "$base/api/public/share/solution/confirm?token=$solutionToken" -Method Post | Out-Null
Write-Host 'PASS solution create/send/customer confirm'

# 3. 报价 + 发送 + 客户在线确认 -> 合同草稿
$quoteBody = @{
    demandId=2; customerId=3; solutionId=$solution.id; productId=$product.id; productName=$product.model
    unitPrice=520000; quantity=1; taxRate=0.13; freight=15000; installFee=30000; floorPrice=500000
    paymentTerms='预付30%，发货前40%，验收后30%'; warranty='12个月'
    validUntil=(Get-Date).AddDays(30).ToString('yyyy-MM-dd')
}
$quote = PostJson '/api/admin/quotes' $quoteBody $admin.token
PostJson "/api/admin/quotes/$($quote.id)/send" @{} $admin.token | Out-Null
$quoteDetail = GetJson "/api/admin/quotes/$($quote.id)" $admin.token
$quoteToken = $quoteDetail.quote.shareToken
Invoke-RestMethod "$base/api/public/share/quote/confirm?token=$quoteToken" -Method Post | Out-Null
Write-Host 'PASS quote create/send/customer confirm -> contract draft'

# 4. 合同审批 + 签署 + 回款 + 验收
$contract = (GetJson "/api/admin/quotes/$($quote.id)" $admin.token).contracts[0]
PostJson "/api/admin/contracts/$($contract.id)/approve" @{ approved=$true; comment='链路验收审批通过' } $admin.token | Out-Null
$signed = PostJson "/api/admin/contracts/$($contract.id)/sign" @{ signType='ELECTRONIC' } $admin.token
Write-Host "contract=$($contract.contractNo) status=$($signed.status)"
$contractDetail = GetJson "/api/admin/contracts/$($contract.id)" $admin.token
$firstPlan = $contractDetail.payments[0]
PostJson "/api/admin/contracts/payment-plans/$($firstPlan.id)/receive" @{ amount=$firstPlan.amount } $admin.token | Out-Null
$acceptance = $contractDetail.installations | Where-Object { $_.stage -eq 'ACCEPTANCE' } | Select-Object -First 1
$installBody = @{ stage='ACCEPTANCE'; status='DONE'; planDate=$acceptance.planDate; actualDate=(Get-Date).ToString('yyyy-MM-dd'); acceptanceSigner='客户张伟'; remark='链路验收安装地址'; photos=''; acceptanceUrl='' }
PostJson "/api/admin/contracts/$($contract.id)/installations/$($acceptance.id)/update" $installBody $admin.token | Out-Null
$after = GetJson "/api/admin/contracts/$($contract.id)" $admin.token
if (-not $after.equipment -or $after.equipment.Count -lt 1) { throw 'equipment not generated after acceptance' }
Write-Host 'PASS contract approve/sign/payment/acceptance/equipment'

# 5. PDF 模板
foreach ($path in @("/api/admin/contracts/$($contract.id)/pdf", "/api/admin/contracts/$($contract.id)/acceptance-pdf", "/api/admin/contracts/$($contract.id)/installation-pdf")) {
    $file = Join-Path $env:TEMP ((Split-Path $path -Leaf) + '-flow.pdf')
    Invoke-WebRequest "$base$path" -Headers (AuthHeaders $admin.token) -OutFile $file
    if ((Get-Item $file).Length -lt 10000) { throw "pdf too small: $path" }
}
Write-Host 'PASS contract/acceptance/installation PDF'

Write-Host "FLOW RESULT: contract=$($contract.contractNo), equipment=$($after.equipment[0].equipmentNo)"
