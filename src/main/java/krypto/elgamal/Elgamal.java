package krypto.elgamal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Elgamal {

    static final Random rnd = new Random();

    PrivateKey privateKey;
    PublicKey publicKey;

    public Elgamal(int bits) {
        generateKeys(bits);
    }

    private BigInteger randBigInt(BigInteger min, BigInteger max) {
        return new BigInteger(Math.abs(rnd.nextInt() % max.bitLength() + 1), rnd).mod(max).add(min);
    }

    private BigInteger findPrimitiveRoot(BigInteger p) {
        if (p.equals(BigInteger.valueOf(2))) {
            return BigInteger.ONE;
        }

        BigInteger p1 = BigInteger.valueOf(2);
        BigInteger p2 = p.subtract(BigInteger.ONE).divide(p1);

        while (true) {
            BigInteger g = randBigInt(BigInteger.valueOf(2), p.subtract(BigInteger.ONE));

            if (!g.modPow(p.subtract(BigInteger.ONE).divide(p1), p).equals(BigInteger.ONE)) {
                if (!g.modPow(p.subtract(BigInteger.ONE).divide(p2), p).equals(BigInteger.ONE)) {
                    return g;
                }
            }
        }
    }

    public List<BigInteger> encode(String plainText, int bits) {
        byte[] bytes = plainText.getBytes();
        List<BigInteger> z = new ArrayList<>();
        int k = bits / 8;
        int j = -k;

        for (int i = 0; i < bytes.length; i++) {
            if (i % k == 0) {
                j += k;
                z.add(BigInteger.ZERO);
            }

            BigInteger mul = BigInteger.valueOf(2).pow(8 * (i % k));
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
                    temp = temp.mod(BigInteger.valueOf(2).pow(8 * j));
                }

                BigInteger letter = temp.divide(BigInteger.valueOf(2).pow(8 * i));
                bytes.add(letter.byteValueExact());

                letter = letter.multiply(BigInteger.valueOf(2).pow(8 * i));
                num = num.subtract(letter);
            }
        }

        byte[] byteArr = new byte[bytes.size()];
        for (int i = 0; i < byteArr.length; i++) {
            byteArr[i] = bytes.get(i);
        }

        return new String(byteArr);
    }

    public void generateKeys(int bits) {
        BigInteger p = BigInteger.probablePrime(bits, rnd);
        BigInteger g = findPrimitiveRoot(p).modPow(BigInteger.valueOf(2), p);
        BigInteger x = randBigInt(BigInteger.ONE, p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)));
        BigInteger h = g.modPow(x, p);

        publicKey = new PublicKey(p, g, h, bits);
        privateKey = new PrivateKey(p, g, x, bits);
    }

    public String encrypt(String plainText) {
        List<BigInteger> z = encode(plainText, publicKey.iNumBits);

        StringBuilder encryptedStr = new StringBuilder();
        for (BigInteger i : z) {
            BigInteger y = randBigInt(BigInteger.ZERO, publicKey.p);
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
            BigInteger smod = s.modPow(privateKey.p.subtract(BigInteger.valueOf(2)), privateKey.p);
            BigInteger plain = d.multiply(smod).mod(privateKey.p);
            encodedText.add(plain);
        }

        return decode(encodedText, privateKey.iNumBits).replace("\0", "");
    }
}
