<?php
// Dev/test helper: signs a Firebase-shaped ID token with a throw-away key and installs the matching
// certificate in the verifier's cache, so the server accepts it without calling Google.
// usage: php make_token.php <projectId> <uid> <provider> [expOffsetSeconds]
declare(strict_types=1);

[, $project, $uid, $provider] = $argv;
$offset = (int) ($argv[4] ?? 3600);
$cacheFile = sys_get_temp_dir() . '/doradodo_firebase_certs.json';
$keyFile = sys_get_temp_dir() . '/doradodo_test_key.pem';

if (!is_readable($keyFile)) {
    $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
    openssl_pkey_export($key, $pem);
    file_put_contents($keyFile, $pem);
}
$key = openssl_pkey_get_private(file_get_contents($keyFile));
$csr = openssl_csr_new(['commonName' => 'test'], $key);
$x509 = openssl_csr_sign($csr, null, $key, 30);
openssl_x509_export($x509, $certPem);
file_put_contents($cacheFile, json_encode(['expires' => time() + 3600, 'certs' => ['testkid' => $certPem]]));

$b64 = fn(string $s) => rtrim(strtr(base64_encode($s), '+/', '-_'), '=');
$now = time();
$h = $b64(json_encode(['alg' => 'RS256', 'kid' => 'testkid', 'typ' => 'JWT']));
$p = $b64(json_encode([
    'iss' => "https://securetoken.google.com/$project", 'aud' => $project, 'sub' => $uid,
    'iat' => $now - 10, 'exp' => $now + $offset, 'firebase' => ['sign_in_provider' => $provider],
]));
openssl_sign("$h.$p", $sig, $key, OPENSSL_ALGO_SHA256);
echo "$h.$p." . $b64($sig);
