package krypto.elgamal;

import java.math.BigInteger;

public class PrivateKey {

    BigInteger p;
    BigInteger g;
    BigInteger x;
    int iNumBits;

    public PrivateKey(BigInteger p, BigInteger g, BigInteger x, int iNumBits) {
        this.p = p;
        this.g = g;
        this.x = x;
        this.iNumBits = iNumBits;
    }
}
