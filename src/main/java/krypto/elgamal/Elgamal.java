package krypto.elgamal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Elgamal {

    private static final Random RND = new Random();
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.valueOf(2);

    public static BigInteger randBigInt(BigInteger min, BigInteger max) {
        int len = Math.abs(RND.nextInt() % max.bitLength() + 1);
        return new BigInteger(len, RND).mod(max.subtract(min).add(ONE)).add(min);
    }

    public static int jacobi(BigInteger a, BigInteger n) {
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
            BigInteger nMod8 = n.mod(eight);

            if (nMod8.equals(ONE) || nMod8.equals(BigInteger.valueOf(7)))
                return 1;

            if (nMod8.equals(three) || nMod8.equals(BigInteger.valueOf(5)))
                return -1;
        }

        if (a.compareTo(n) >= 0)
            return jacobi(a.mod(n), n);

        if (a.mod(TWO).equals(ZERO))
            return jacobi(TWO, n) * jacobi(a.divide(TWO), n);

        if (a.mod(four).equals(three) && n.mod(four).equals(three))
            return -jacobi(n, a);

        return jacobi(n, a);
    }

    public static boolean SolovayStrassen(BigInteger num, int confidence) {
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

    public static BigInteger findPrime(int bits, int confidence) {
        while (true) {
            BigInteger candidate;

            do candidate = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));
            while (candidate.mod(TWO).equals(ZERO));

            while (!SolovayStrassen(candidate, confidence)) {
                do candidate = randBigInt(TWO.pow(bits - 2), TWO.pow(bits - 1));
                while (candidate.mod(TWO).equals(ZERO));
            }

            candidate = candidate.multiply(TWO).add(ONE);
            if (SolovayStrassen(candidate, confidence))
                return candidate;
        }
    }

    public static BigInteger findPrimitiveRoot(BigInteger p) {
        if (p.equals(TWO))
            return ONE;

        BigInteger p2 = p.subtract(ONE).divide(TWO);

        while (true) {
            BigInteger g = randBigInt(TWO, p.subtract(ONE));

            if (!g.modPow(p.subtract(ONE).divide(TWO), p).equals(ONE))
                if (!g.modPow(p.subtract(ONE).divide(p2), p).equals(ONE))
                    return g;
        }
    }

    public static void generateKeys(int bits, int confidence, PrivateKey privKey, PublicKey pubKey) {
        pubKey.p = findPrime(bits, confidence);
        pubKey.g = findPrimitiveRoot(pubKey.p).modPow(TWO, pubKey.p);
        privKey.x = randBigInt(ONE, pubKey.p.subtract(ONE).divide(TWO));
        pubKey.h = pubKey.g.modPow(privKey.x, pubKey.p);

        privKey.p = pubKey.p;
        privKey.g = pubKey.g;
        pubKey.bits = pubKey.p.bitLength();
        privKey.bits = pubKey.bits;
    }

    public static List<BigInteger> encode(String plainText, int bits) {
        int charBits = 8;
        int packets = bits / (charBits + 1);
        byte[] bytes = plainText.getBytes();
        List<BigInteger> z = new ArrayList<>();

        int index = -1;
        for (int i = 0; i < bytes.length; i++) {
            if (i % packets == 0) {
                index++;
                z.add(ZERO);
            }

            BigInteger mul = TWO.pow(charBits * (i % packets));
            BigInteger add = BigInteger.valueOf(bytes[i] & 0xFF).multiply(mul);
            z.set(index, z.get(index).add(add));
        }

        return z;
    }

    public static String decode(List<BigInteger> encodedText, int bits) {
        int charBits = 8;
        int packets = bits / (charBits + 1);
        List<Byte> bytes = new ArrayList<>();

        for (BigInteger num : encodedText) {
            for (int j = 0; j < packets; j++) {
                BigInteger temp = num;

                for (int k = j + 1; k < packets; k++)
                    temp = temp.mod(TWO.pow(charBits * k));

                BigInteger letter = temp.divide(TWO.pow(charBits * j));
                bytes.add(letter.byteValue());
                num = num.subtract(letter.multiply(TWO.pow(charBits * j)));
            }

        }

        bytes = bytes.stream().filter(b -> b != 0).collect(Collectors.toList());

        byte[] bytesArr = new byte[bytes.size()];
        for (int i = 0; i < bytesArr.length; i++) {
            bytesArr[i] = bytes.get(i);
        }

        return new String(bytesArr);
    }

    public static String encrypt(String plainText, int radix, PublicKey key) {
        List<BigInteger> z = encode(plainText, key.bits);

        StringBuilder encryptedStr = new StringBuilder();
        for (BigInteger i : z) {
            BigInteger y = randBigInt(ONE, key.p);
            BigInteger c = key.g.modPow(y, key.p);
            BigInteger hmod = key.h.modPow(y, key.p);
            BigInteger d = i.multiply(hmod).mod(key.p);
            encryptedStr.append(c.toString(radix)).append(" ")
                    .append(d.toString(radix)).append(" ");
        }

        return encryptedStr.toString();
    }

    public static String decrypt(String cipher, int radix, PrivateKey key) {
        String[] cipherArray = cipher.split(" ");
        List<BigInteger> encodedText = new ArrayList<>();

        for (int i = 0; i < cipherArray.length; i += 2) {
            BigInteger c = new BigInteger(cipherArray[i], radix);
            BigInteger d = new BigInteger(cipherArray[i + 1], radix);
            BigInteger s = c.modPow(key.x, key.p);
            BigInteger srev = s.modPow(key.p.subtract(TWO), key.p);
            BigInteger plain = d.multiply(srev).mod(key.p);
            encodedText.add(plain);
        }

        return decode(encodedText, key.bits);
    }

    public static class PrivateKey {
        BigInteger p;
        BigInteger g;
        BigInteger x;
        int bits;
    }

    public static class PublicKey {
        BigInteger p;
        BigInteger g;
        BigInteger h;
        int bits;
    }
}