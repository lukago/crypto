#include <iostream>
#include <algorithm>
#include <iterator>
#include <sstream>
#include <fstream>
#include <cstring>
#include <chrono>

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

        int j;
        for (j = 0; j < r - 1; j++) {
            x = modPow(x, 2, n);
            if (x == 1) return false;
            if (x == n - 1) break;
        }

        if (j >= r - 1)
            return false;
    }

    return true;
}

BigInt findPrime(int confidence, const BigInt &min, const BigInt &max)
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
    BigInt p = findPrime(confidence, fastPow(2, bits - 1), fastPow(2, bits));
    BigInt g = xrand(fastPow(2, bits - 1), p);
    BigInt x = xrand(1, p - 1);
    BigInt h = modPow(g, x, p);

    PublicKey pub{p, g, h, bits};
    PrivateKey priv{p, g, x, bits};
    return make_pair(priv, pub);
}

vector<BigInt> encode(const string &plainText, int bits)
{
    int charBits = 8;
    int packets = bits / (charBits + 1);
    vector<char> bytes(plainText.begin(), plainText.end());
    vector<BigInt> encoded;

    int index = -1;
    for (int i = 0; i < bytes.size(); i++) {
        if (i % packets == 0) {
            index++;
            encoded.emplace_back(0);
        }

        encoded[index] += fastPow(2, charBits * (i % packets)) * (bytes[i] & 0xFF);
    }

    return encoded;
}

string decode(const vector<BigInt> &encodedText, int bits)
{
    int charBits = 8;
    int packets = bits / (charBits + 1);
    vector<char> bytes;

    for (BigInt num : encodedText) {
        for (int j = 0; j < packets; j++) {
            BigInt temp = num;

            for (int k = j + 1; k < packets; k++)
                temp = temp % fastPow(2, charBits * k);

            BigInt letter = temp / fastPow(2, charBits * j);
            bytes.push_back((char) letter.longValue());
            num -= letter * fastPow(2, charBits * j);
        }
    }

    vector<char> bytesF;
    copy_if(bytes.begin(), bytes.end(), back_inserter(bytesF), [](char i) {
        return i != 0;
    });

    return string(bytesF.begin(), bytesF.end());
}

string encrypt(const string &plainText, PublicKey key, int radix)
{
    vector<BigInt> z = encode(plainText, key.bits);

    BigInt y, c, s, d;
    string encryptedStr;
    for (const BigInt &i : z) {
        y = xrand(1, key.p);
        c = modPow(key.g, y, key.p);
        s = modPow(key.h, y, key.p);
        d = (i * s) % key.p;
        encryptedStr.append(c.toString(radix) + " " + d.toString(radix) + " ");
    }

    return encryptedStr;
}

string decrypt(const string &cipher, PrivateKey key, int radix)
{
    vector<BigInt> encodedText;
    BigInt c, d, s, srev, plain;
    stringstream ss(cipher);
    vector<string> cipherVec{istream_iterator<string>{ss}, istream_iterator<string>{}};

    for (int i = 0; i < cipherVec.size(); i += 2) {
        c = BigInt(cipherVec[i], radix);
        d = BigInt(cipherVec[i + 1], radix);
        s = modPow(c, key.x, key.p);
        srev = modPow(s, key.p - 2, key.p);
        plain = (d * srev) % key.p;
        encodedText.push_back(plain);
    }

    return decode(encodedText, key.bits);
}

// input/output utils
void saveToFile(const vector<string> &data, const string &fileName);
void saveMsgToFile(const string &msg, const string &fileName);
vector<string> readFile(const string &fileName);
string readMsg(const string &fileName);
vector<string> privKeyToVec(const PrivateKey &key, int radix);
vector<string> pubKeyToVec(const PublicKey &key, int radix);
PrivateKey privKeyFromVec(const vector<string> &data, int radix);

// ====================================================================
// =============================== MAIN ===============================
// ====================================================================
int main(int argc, char **argv)
{
    srand(time(nullptr));

    if (argc > 0 && strcmp(argv[1], "encr") == 0) {
        string msgPath = argv[2];
        int bits = stoi(argv[3]);
        int conf = stoi(argv[4]);
        int radix = stoi(argv[5]);
        string pubKeyPath = argv[6];
        string privKeyPath = argv[7];
        string cipherPath = argv[8];

        auto keys = generateKeys(bits, conf);
        string cipher = encrypt(readMsg(msgPath), keys.second, radix);

        saveToFile(privKeyToVec(keys.first, radix), privKeyPath);
        saveToFile(pubKeyToVec(keys.second, radix), pubKeyPath);
        saveMsgToFile(cipher, cipherPath);

        cout << "Encrypted.";
        return 0;
    }

    if (argc > 0 && strcmp(argv[1], "decr") == 0) {
        string cipherPath = argv[2];
        string privKeyPath = argv[3];
        int radix = stoi(argv[4]);
        string decmsgPath = argv[5];

        PrivateKey privateKey = privKeyFromVec(readFile(privKeyPath), radix);
        string msg = decrypt(readMsg(cipherPath), privateKey, radix);

        saveMsgToFile(msg, decmsgPath);

        cout << "Decrypted.";
        return 0;
    }

    string usage = "Usage:\n"
            "encr {path to msg} {prime bits num} {prime test confidence} {radix} "
            "{path to save pub key} {path to save priv key} {path to save cipher}\n"
            "decr {path to cipher} {priv key path} {radix} {path to save decrypted message}";

    cout << usage;
    return 0;
}

void saveToFile(const vector<string> &data, const string &fileName)
{
    ofstream out(fileName);
    for (const auto &i : data) out << hex << i << "\n";
    out.close();
}

void saveMsgToFile(const string &msg, const string &fileName)
{
    ofstream out(fileName);
    out << hex << msg;
    out.close();
}

vector<string> readFile(const string &fileName)
{
    string line;
    ifstream in(fileName);
    vector<string> data;
    while (getline(in, line))
        data.push_back(line);
    in.close();
    return data;
}

string readMsg(const string &fileName)
{
    ifstream in(fileName);
    std::stringstream buffer;
    buffer << in.rdbuf();
    string msg = buffer.str();
    in.close();
    return msg;
}

vector<string> privKeyToVec(const PrivateKey &key, int radix)
{
    return vector<string>{
            key.p.toString(radix),
            key.g.toString(radix),
            key.x.toString(radix),
            to_string(key.bits)
    };
}

vector<string> pubKeyToVec(const PublicKey &key, int radix)
{
    return vector<string>{
            key.p.toString(radix),
            key.g.toString(radix),
            key.h.toString(radix),
            to_string(key.bits)
    };
}

PrivateKey privKeyFromVec(const vector<string> &data, int radix)
{
    return PrivateKey{
            BigInt(data[0], radix),
            BigInt(data[1], radix),
            BigInt(data[2], radix),
            stoi(data[3])
    };
}