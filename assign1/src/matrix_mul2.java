import java.text.DecimalFormat;
import java.util.Scanner;

class Application {
    public static void onMult(int m_ar, int m_br) {

        long time1, time2;

        String timestr;
        double temp;
        int i, j, k;

        double[] pha, phb, phc;

        pha = new double[m_ar * m_ar];
        phb = new double[m_ar * m_ar];
        phc = new double[m_ar * m_ar];

        for (i = 0; i < m_ar; i++) {
            for (j = 0; j < m_ar; j++) {
                pha[i * m_ar + j] = (double) 1.0;
            }
        }

        for (i = 0; i < m_br; i++) {
            for (j = 0; j < m_br; j++) {
                phb[i * m_br + j] = (double) (i + 1);
            }
        }

        time1 = System.currentTimeMillis();

        for (i = 0; i < m_ar; i++) {
            for (j = 0; j < m_br; j++) {
                temp = 0;
                for (k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }

        time2 = System.currentTimeMillis();
        timestr = "Time: " + new DecimalFormat("#0.000").format((double) (time2 - time1) / 1000) + " seconds";
        System.out.println(timestr);

        // display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix: ");
        for (i = 0; i < 1; i++) {
            for (j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    public static void onMultLine(int m_ar, int m_br) {

        long time1, time2;

        String timestr;
        int i, j, k;

        double[] pha, phb, phc;

        pha = new double[m_ar * m_ar];
        phb = new double[m_ar * m_ar];
        phc = new double[m_ar * m_ar];

        for (i = 0; i < m_ar; i++) {
            for (j = 0; j < m_ar; j++) {
                pha[i * m_ar + j] = (double) 1.0;
            }
        }

        for (i = 0; i < m_br; i++) {
            for (j = 0; j < m_br; j++) {
                phb[i * m_br + j] = (double) (i + 1);
            }
        }

        for (i = 0; i < m_br; i++) {
            for (j = 0; j < m_br; j++) {
                phc[i * m_br + j] = (double) (0.0);
            }
        }

        time1 = System.currentTimeMillis();

        for (i = 0; i < m_ar; i++) {
            for (k = 0; k < m_br; k++) {
                for (j = 0; j < m_ar; j++) {
                    phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                }
            }
        }

        time2 = System.currentTimeMillis();
        timestr = "Time: " + new DecimalFormat("#0.000").format((double) (time2 - time1) / 1000) + " seconds";
        System.out.println(timestr);

        // display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix: ");
        for (i = 0; i < 1; i++) {
            for (j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number of rows and columns: ");
        int lin = sc.nextInt();
        int col = sc.nextInt();
        System.out.println("1- OnMult 2- OnLineMult");
        int opt = sc.nextInt();
        switch (opt) {
            case 1: {
                onMult(lin, col);
                break;
            }
            case 2: {
                onMultLine(lin, col);
                break;
            }
            default: {
                System.out.println("Invalid opt");
            }
        }
        sc.close();
    }
}