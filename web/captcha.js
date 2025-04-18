/**
 *  Calculates the SHA-256 hash of a string using SubtleCrypto
 *  and returns it as a hexadecimal string.
 *
 *  @param {string} str - The input string.
 *  @returns {Promise<string>} A Promise that resolves with the SHA-256 hash as a 64-character hex string.
 *  @throws {Error} If SubtleCrypto is not available.
 */
async function sha256_hex(str)
{
    if (!crypto || !crypto.subtle || !crypto.subtle.digest)
    {
        throw new Error("SubtleCrypto API not available in this browser/context.");
    }
    if (typeof TextEncoder === 'undefined')
    {
        throw new Error("TextEncoder API not available in this browser/context.");
    }

    // Encode the string into a Uint8Array -> ArrayBuffer
    const dataBuffer = new TextEncoder().encode(str);

    // Calculate the hash using SubtleCrypto
    const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);

    // Convert the ArrayBuffer to a hexadecimal string
    const hashArray = Array.from(new Uint8Array(hashBuffer)); // Convert buffer to byte array
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join(''); // Convert bytes to hex

    return hashHex;
}

/**
 *  Solves a Proof-of-Work (PoW) challenge using SHA-256 via SubtleCrypto.
 *  Finds a nonce such that the SHA-256 hash of (challenge + nonce)
 *  starts with a specified number of zero characters (hexadecimal).
 *
 *  THIS FUNCTION IS ASYNCHRONOUS due to SubtleCrypto usage.
 *
 *  @param {string} challenge - A unique string for this specific PoW task.
 *  @param {number} [difficulty=3] - The required number of leading zero hex characters (0-64).
 *  Higher numbers are exponentially harder. **Requires tuning for desired runtime.**
 *  @returns {Promise<{nonce: number, hash: string, duration: number, challenge: string, difficulty: number}>}
 *  A Promise resolving to an object containing the found nonce, the resulting 64-char hex hash,
 *  the time taken in milliseconds, the original challenge, and the difficulty used.
 *  @throws {Error} If the challenge is not a non-empty string, difficulty is invalid,
 *  or SubtleCrypto/TextEncoder is unavailable.
 */
async function solvePoWCaptchaSHA256(challenge, difficulty = 3)
{
    if (typeof challenge !== 'string' || challenge.length === 0)
    {
        throw new Error("PoW challenge must be a non-empty string.");
    }
    // SHA-256 produces a 64-character hex string (256 bits)
    if (!Number.isInteger(difficulty) || difficulty <= 0 || difficulty > 64)
    {
        throw new Error("PoW difficulty must be a positive integer between 1 and 64.");
    }
    // Check for SubtleCrypto availability early
    if (!crypto || !crypto.subtle || !crypto.subtle.digest || typeof TextEncoder === 'undefined')
    {
        throw new Error("Required cryptographic APIs (SubtleCrypto, TextEncoder) not available.");
    }

    console.log(`Starting SHA-256 PoW solve: Challenge="${challenge}", Difficulty=${difficulty}`);
    const startTime = performance.now();

    const targetPrefix = '0'.repeat(difficulty);
    let nonce = 0;
    let hash = '';
    let data = '';

    // Loop until a valid hash is found
    while (true)
    {
        data = nonce + challenge; // Concatenate challenge and current nonce
        hash = await sha256_hex(data); // Calculate the SHA-256 hash (asynchronously)

        if (hash.startsWith(targetPrefix)) 
        {
            // Solution found!
            const endTime = performance.now();
            const duration = endTime - startTime;
            console.log(`SHA-256 PoW Solved! Nonce: ${nonce}, Hash: ${hash}, Time: ${duration.toFixed(2)} ms`);

            return {
                nonce: nonce,
                hash: hash,
                duration: duration,
                challenge: challenge,
                difficulty: difficulty
            };
        }

        nonce++; // Increment nonce and try again
    }
}

// Everything above is courtesy of Gemini 2.5 Pro
const date = new Date().toISOString();
var captcha = solvePoWCaptchaSHA256(date + window.chl);
const temp = new URL(window.location.href);
captcha.then((result) => {
    temp.searchParams.set("powans", result.hash);
    temp.searchParams.set("nonce", result.nonce);
    temp.searchParams.set("powts", date);
    temp.searchParams.set("powdif", result.difficulty);
    window.location.href = temp.toString();
});