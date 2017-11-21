package krypto.elgamal;

import java.math.BigInteger;

public class PublicKey {

    BigInteger p;
    BigInteger g;
    BigInteger h;
    int iNumBits;

    public PublicKey(BigInteger p, BigInteger g, BigInteger h, int iNumBits) {
        this.p = p;
        this.g = g;
        this.h = h;
        this.iNumBits = iNumBits;
    }
}
