import java.util.Scanner;

public class test4{
    static  int N = 1;
    public static void main(String[]args) throws Exception {
        N = System.in.available();
        int n=sc.nextInt();
        int m=sc.nextInt();

        boolean[]variables=new boolean[n];
        int[]gates=new int[m];
        int[]first=new int[m];
        int[]second=new int[m];


        for(int i=0;i<Math.min(N,n);i++){
            variables[i]=sc.nextInt()==1;
        }
        for(int i=0;i<Math.min(N,m);i++){
            gates[i]=sc.nextInt();
            first[i]=sc.nextInt();
            if(gates[i]!=2)
                second[i]=sc.nextInt();
        }
        System.out.println(CV(N,variables,gates,first,second));
    }
    static boolean CV(int Lim, boolean[] variables, int[] gates, int[] firstInputs, int[] secondInputs){
        int n=variables.length;
        int m=gates.length;
        boolean[] results=new boolean[n+m];
        for(int i=0;i<Math.min(Lim,n);i++){
            results[i]=variables[i];
        }

        for(int i=0;i<Math.min(Lim,m);i++){
            if(gates[i]==0){
                results[i+n]=results[firstInputs[i]]|results[secondInputs[i]];
            }else if(gates[i]==1){
                results[i+n]=results[firstInputs[i]]&results[secondInputs[i]];
            }else{
                results[i+n]=!results[firstInputs[i]];
            }
        }

        return results[n+m-1];
    }
    static Scanner sc=new Scanner(System.in);
}
