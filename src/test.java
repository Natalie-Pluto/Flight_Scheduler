public class test {
    public static void main(String[] args) {
        tri(4);
    }
    public static void tri(int x) {
        int k = x;
        int j = k - 1;
        boolean finish = false;
        while(!finish) {
            for (int i = 0; i < k - j; i++) {
                System.out.print("x");
                if (j < 1) {
                    finish = true;
                }
            }
            j--;
            System.out.println(" ");
        }

    }

}
