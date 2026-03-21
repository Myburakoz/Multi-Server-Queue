package util;

public class ManualRandom {
    public static int randRange(int a, int b) {
        if (a > b) {
            int temp = a;
            a = b;
            b = temp;
        }
        return a + (int)(Math.random() * (b - a + 1));
    }
}
