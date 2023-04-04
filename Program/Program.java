
public class Program {
    static int N = 100;

    public static void main(String[]args) throws Exception {

        bla(N);

    }

    static int bla(int Lim) {
        if(Lim<=0)return 1;
        return bla(Lim-1) + bla(Lim-2);
    }
}