package krypto.elgamal;

public class App {

    public static void main(String[] args) {
        int kradix = 36;
        int radix = 36;
        String text = "Przykładowy ukryty tekst...abc def ghf ijk @!123ŁÓÐŹ";

        Elgamal.PrivateKey privateKey = new Elgamal.PrivateKey();
        Elgamal.PublicKey publicKey = new Elgamal.PublicKey();
        Elgamal.generateKeys(512, 32, privateKey, publicKey);

        System.out.println("Public key:");
        System.out.println(publicKey.p.toString(kradix));
        System.out.println(publicKey.g.toString(kradix));
        System.out.println(publicKey.h.toString(kradix));

        System.out.println("Private key:");
        System.out.println(privateKey.p.toString(kradix));
        System.out.println(privateKey.g.toString(kradix));
        System.out.println(privateKey.x.toString(kradix));

        String cipher = Elgamal.encrypt(text, radix, publicKey);
        String decrypt = Elgamal.decrypt(cipher, radix, privateKey);

        System.out.println(cipher);
        System.out.println(decrypt);
    }
}
