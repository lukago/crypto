#include <iostream>

#include "BigInt.h"
#include "primes.h"

using namespace std;

struct PrivateKey
{
    BigInt p;
    BigInt g;
    BigInt x;
    int bits;
};

struct PublicKey
{
    BigInt p;
    BigInt g;
    BigInt h;
    int bits;
};

bool trialDiv(const BigInt &n)
{
    const int *p = &primes[0];
    for (int i = 0; i < primesNum - 1; ++i) {
        if (n % *(p++) == 0) return true;
    }

    return false;
}

bool millerRabin(const BigInt &n, int k)
{
    BigInt d = n - 1;
    int r = 0;
    while (d % 2 == 0) {
        r++;
        d /= 2;
    }

    for (int i = 0; i < k; i++) {
        BigInt a = xrand(2, n - 1);
        BigInt x = modPow(a, d, n);

        if (x == 1 || x == n - 1) continue;

        for (int j = 0; j < r - 1; j++) {
            x = modPow(x, 2, n);
            if (x == 1) return false;
            if (x == n - 1) break;
        }

        return false;
    }

    return true;
}

BigInt findPrimie(int confidence, const BigInt &min, const BigInt &max)
{
    BigInt candidate;

    do candidate = xrand(min, max);
    while (trialDiv(candidate));

    while (!millerRabin(candidate, confidence)) {
        do candidate = xrand(min, max);
        while (trialDiv(candidate));
    }

    return candidate;
}

pair<PrivateKey, PublicKey> generateKeys(int bits, int confidence)
{
    BigInt p = findPrimie(confidence, fastPow(2, bits - 1), fastPow(2, bits));
    BigInt g = xrand(fastPow(2, bits - 1), p);
    BigInt x = xrand(1, p - 1);
    BigInt h = modPow(g, x, p);

    PublicKey pub{p, g, h, bits};
    PrivateKey priv{p, g, x, bits};
    return make_pair(priv, pub);
}

int main()
{
    srand(time(0));

    auto res = generateKeys(1024, 5);

    cout << res.first.p << endl;
    cout << res.first.g << endl;
    cout << res.first.x << endl << endl;

    cout << res.second.p << endl;
    cout << res.second.g << endl;
    cout << res.second.h << endl;

    return 0;
}