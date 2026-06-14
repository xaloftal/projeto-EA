param(
    [ValidateSet("smoke", "baseline", "stress")]
    [string]$Profile = "smoke"
)

$ErrorActionPreference = "Stop"

function Get-ProfileConfig([string]$name) {
    switch ($name) {
        "smoke" {
            return @{
                CheckoutThreads = 5
                CheckoutRamp = 10
                CheckoutLoops = -1
                CheckoutDuration = 60
                PaymentThreads = 10
                PaymentRamp = 10
                PaymentLoops = -1
                PaymentDuration = 60
                Cpu = "1.0"
                Memory = "1024m"
            }
        }
        "baseline" {
            return @{
                CheckoutThreads = 8
                CheckoutRamp = 30
                CheckoutLoops = -1
                CheckoutDuration = 180
                PaymentThreads = 20
                PaymentRamp = 30
                PaymentLoops = -1
                PaymentDuration = 180
                Cpu = "1.5"
                Memory = "1536m"
            }
        }
        "stress" {
            return @{
                CheckoutThreads = 15
                CheckoutRamp = 40
                CheckoutLoops = -1
                CheckoutDuration = 300
                PaymentThreads = 40
                PaymentRamp = 40
                PaymentLoops = -1
                PaymentDuration = 300
                Cpu = "2.0"
                Memory = "2048m"
            }
        }
    }
}

$config = Get-ProfileConfig -name $Profile

Write-Host "Running profile: $Profile"
Write-Host "Container limits: CPU=$($config.Cpu), Memory=$($config.Memory)"

$checkoutResult = "/tests/load-tests/jmeter/results-checkout-$Profile.jtl"
$paymentResult = "/tests/load-tests/jmeter/results-payment-$Profile.jtl"

$checkoutCmd = @(
    "run", "--rm",
    "--cpus", $config.Cpu,
    "--memory", $config.Memory,
    "-v", "${PWD}:/tests",
    "justb4/jmeter:latest",
    "-n",
    "-t", "/tests/load-tests/jmeter/catchit-checkout-flow.jmx",
    "-Jhost=host.docker.internal",
    "-Jport=8080",
    "-Jthreads=$($config.CheckoutThreads)",
    "-Jramp=$($config.CheckoutRamp)",
    "-Jloops=$($config.CheckoutLoops)",
    "-JuseScheduler=true",
    "-Jduration=$($config.CheckoutDuration)",
    "-JusersFile=/tests/load-tests/jmeter/users.csv",
    "-JresultsFile=$checkoutResult",
    "-l", $checkoutResult
)

docker @checkoutCmd

$paymentCmd = @(
    "run", "--rm",
    "--cpus", $config.Cpu,
    "--memory", $config.Memory,
    "-v", "${PWD}:/tests",
    "justb4/jmeter:latest",
    "-n",
    "-t", "/tests/load-tests/jmeter/catchit-payment-service.jmx",
    "-Jhost=host.docker.internal",
    "-Jport=8081",
    "-Jthreads=$($config.PaymentThreads)",
    "-Jramp=$($config.PaymentRamp)",
    "-Jloops=$($config.PaymentLoops)",
    "-JuseScheduler=true",
    "-Jduration=$($config.PaymentDuration)",
    "-JresultsFile=$paymentResult",
    "-l", $paymentResult
)

docker @paymentCmd

Write-Host "Done."
Write-Host "Checkout result: load-tests/jmeter/results-checkout-$Profile.jtl"
Write-Host "Payment result: load-tests/jmeter/results-payment-$Profile.jtl"
