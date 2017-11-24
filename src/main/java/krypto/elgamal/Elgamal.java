package krypto.elgamal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Elgamal {

    static final Random RND = new Random();
    static final BigInteger ZERO = BigInteger.ZERO;
    static final BigInteger ONE = BigInteger.ONE;
    static final BigInteger TWO = BigInteger.valueOf(2);

    PrivateKey privateKey;
    PublicKey publicKey;

    public Elgamal(int bits, int confidence) {
        generateKeys(bits, confidence);
    }

    public BigInteger randBigInt(BigInteger min, BigInteger max) {
        int len = Math.abs(RND.nextInt() % max.bitLength() + 1);
        return new BigInteger(len, RND).mod(max).add(min);
    }

    public int jacobi(BigInteger a, BigInteger n) {
        BigInteger three = BigInteger.valueOf(3);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger eight = BigInteger.valueOf(8);

        if (a.equals(ZERO))
            return n.equals(ONE) ? 1 : 0;

        if (a.equals(BigInteger.valueOf(-1)))
            return n.mod(TWO).equals(ZERO) ? 1 : -1;

        if (a.equals(ONE))
            return 1;

        if (a.equals(TWO)) {
            if (n.mod(eight).equals(ONE) || n.mod(eight).equals(BigInteger.valueOf(7)))
                return 1;
            if (n.mod(eight).equals(three) || n.mod(eight).equals(BigInteger.valueOf(5)))
                return -1;
        }

        if (a.compareTo(n) >= 0)
            return jacobi(a.mod(n), n);

        if (a.mod(TWO).equals(ZERO))
            return jacobi(TWO, n) * jacobi(a.divide(TWO), n);

        if (a.mod(four).equals(three) && n.mod(four).equals(three))
            return -1 * jacobi(n, a);

        return jacobi(n, a);
    }

    boolean SolovayStrassen(BigInteger num, int confidence) {
        for (int i = 0; i < confidence; i++) {
            BigInteger a = randBigInt(ONE, num.subtract(ONE));

            if (a.gcd(num).compareTo(ONE) > 0)
                return false;

            BigInteger left = a.modPow(num.subtract(ONE).divide(TWO), num);
            BigInteger jacobi = BigInteger.valueOf(jacobi(a, num));
            if (!jacobi.mod(num).equals(left))
                return false;
        }

        return true;
    }

    public BigInteger findPrime(int bits, int confidence) {
        while (true) {
            BigInteger p = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));

            while (p.mod(TWO).equals(ZERO))
                p = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));

            while (!SolovayStrassen(p, confidence)) {
                p = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));
                while (p.mod(TWO).equals(ZERO))
                    p = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));
            }

            p = p.multiply(TWO).add(ONE);
            if (SolovayStrassen(p, confidence))
                return p;
        }
    }

    private BigInteger findPrimitiveRoot(BigInteger p) {
        if (p.equals(TWO)) {
            return ONE;
        }

        BigInteger p1 = TWO;
        BigInteger p2 = p.subtract(ONE).divide(p1);

        while (true) {
            BigInteger g = randBigInt(TWO, p.subtract(ONE));

            if (!g.modPow(p.subtract(ONE).divide(p1), p).equals(ONE)) {
                if (!g.modPow(p.subtract(ONE).divide(p2), p).equals(ONE)) {
                    return g;
                }
            }
        }
    }

    public void generateKeys(int bits, int confidence) {
        BigInteger p = findPrime(bits, confidence);
        BigInteger g = findPrimitiveRoot(p).modPow(TWO, p);
        BigInteger x = randBigInt(ONE, p.subtract(ONE).divide(TWO));
        BigInteger h = g.modPow(x, p);

        publicKey = new PublicKey(p, g, h, bits);
        privateKey = new PrivateKey(p, g, x, bits);
    }

    public List<BigInteger> encode(String plainText, int bits) {
        byte[] bytes = plainText.getBytes();
        List<BigInteger> z = new ArrayList<>();
        int k = bits / 8;
        int j = -k;

        for (int i = 0; i < bytes.length; i++) {
            if (i % k == 0) {
                j += k;
                z.add(ZERO);
            }

            BigInteger mul = TWO.pow(8 * (i % k));
            BigInteger add = BigInteger.valueOf(bytes[i]).multiply(mul);
            z.set(j / k, z.get(j / k).add(add));
        }

        return z;
    }

    public String decode(List<BigInteger> encodedText, int bits) {
        List<Byte> bytes = new ArrayList<>();
        int k = bits / 8;

        for (BigInteger num : encodedText) {
            for (int i = 0; i < k; i++) {
                BigInteger temp = num;

                for (int j = i + 1; j < k; j++) {
                    temp = temp.mod(TWO.pow(8 * j));
                }

                BigInteger letter = temp.divide(TWO.pow(8 * i));
                bytes.add(letter.byteValueExact());

                letter = letter.multiply(TWO.pow(8 * i));
                num = num.subtract(letter);
            }
        }

        byte[] byteArr = new byte[bytes.size()];
        for (int i = 0; i < byteArr.length; i++) {
            byteArr[i] = bytes.get(i);
        }

        return new String(byteArr);
    }

    public String encrypt(String plainText) {
        List<BigInteger> z = encode(plainText, publicKey.iNumBits);

        StringBuilder encryptedStr = new StringBuilder();
        for (BigInteger i : z) {
            BigInteger y = randBigInt(ZERO, publicKey.p);
            BigInteger c = publicKey.g.modPow(y, publicKey.p);
            BigInteger hmod = publicKey.h.modPow(y, publicKey.p);
            BigInteger d = i.multiply(hmod).mod(publicKey.p);
            encryptedStr.append(c).append(" ").append(d).append(" ");
        }

        return encryptedStr.toString();
    }

    public String decrypt(String cipher) {
        String[] cipherArray = cipher.split(" ");
        List<BigInteger> encodedText = new ArrayList<>();

        for (int i = 0; i < cipherArray.length; i += 2) {
            BigInteger c = new BigInteger(cipherArray[i]);
            BigInteger d = new BigInteger(cipherArray[i + 1]);
            BigInteger s = c.modPow(privateKey.x, privateKey.p);
            BigInteger smod = s.modPow(privateKey.p.subtract(TWO), privateKey.p);
            BigInteger plain = d.multiply(smod).mod(privateKey.p);
            encodedText.add(plain);
        }

        return decode(encodedText, privateKey.iNumBits).replace("\0", "");
    }
}
