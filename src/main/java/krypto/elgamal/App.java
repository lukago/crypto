package krypto.elgamal;

public class App {

    public static void main(String[] args) {
        String text = "Przykladowy ukryty tekst...abc def ghf ijk @!";
        Elgamal elgamal = new Elgamal(8, 100);

        System.out.println("Public key:");
        System.out.println(elgamal.publicKey.p);
        System.out.println(elgamal.publicKey.g);
        System.out.println(elgamal.publicKey.h);

        System.out.println("Private key:");
        System.out.println(elgamal.privateKey.p);
        System.out.println(elgamal.privateKey.g);
        System.out.println(elgamal.privateKey.x + "\n");

        String cipher = elgamal.encrypt(text);
        String decrypt = elgamal.decrypt(cipher);

        System.out.println(cipher);
        System.out.println(decrypt);
    }
}
