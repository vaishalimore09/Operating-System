import java.io.*;

public class MOS1 {
    static char[][] M = new char[100][4];
    static char IR[] = new char[4];
    static int IC[] = new int[2];
    static char R[] = new char[4];
    static boolean C;
    static int SI = 0;
    static String buffer;
    static BufferedReader fin;
    static BufferedWriter fout;

    static void init() {
        for (int i = 0; i < 4; i++) {
            IR[i] = ' ';
            R[i] = ' ';
        }
        IC[0] = 0;
        IC[1] = 0;
        C = false;
        SI = 0;
        buffer = "";
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 4; j++) {
                M[i][j] = '-';
            }
        }
    }

    static void MOS() {
        switch (SI) {
            case 1:
                READ();
                SI = 0;
                break;
            case 2:
                WRITE();
                SI = 0;
                break;
            case 3:
                TERMINATE();
                SI = 0;
                break;
        }
    }

    static void READ() {
        IR[3] = '0';
        int start = (IR[2] - '0') * 10;
        try {
            buffer = fin.readLine();
            int j = 0;
            for (int i = 0; i < buffer.length();) {
                M[start][j] = buffer.charAt(i);
                i++;
                j++;
                if (j == 4) {
                    j = 0;
                    start++;
                }
            }
            buffer = "";
        } catch (IOException e) {
            e.printStackTrace();
        }
        EXECUTEUSERPROGRAM();
    }

    static void WRITE() {
        IR[3] = '0';
        int start = (IR[2] - '0') * 10;
        for (int i = start; i < start + 10; i++) {
            for (int j = 0; j < 4; j++) {
                try {
                    if (M[i][j] == '-') {
                        fout.write("");
                    } else {
                        fout.write(M[i][j]);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            fout.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        EXECUTEUSERPROGRAM();
    }

    static void TERMINATE() {
        try {
            fout.write("\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void STARTEXECUTION() {
        IC[0] = 0;
        IC[1] = 0;
        EXECUTEUSERPROGRAM();
    }

    static void EXECUTEUSERPROGRAM() {
        while (true) {
            int ind = IC[0] * 10 + IC[1];
            for (int i = 0; i < 4; i++) {
                IR[i] = M[ind][i];
            }
            IC[1] += 1;
            if (IC[1] == 10) {
                IC[1] = 0;
                IC[0] += 1;
            }
            String Examine;
            if (IR[0] == 'H') {
                Examine = IR[0] + "";
            } else {
                Examine = IR[0] + "" + IR[1];
            }
            int x = (IR[2] - '0') * 10 + (IR[3] - '0');
            switch (Examine) {
                case "LR":
                    for (int i = 0; i < 4; i++) {
                        R[i] = M[x][i];
                    }
                    break;
                case "SR":
                    for (int i = 0; i < 4; i++) {
                        M[x][i] = R[i];
                    }
                    break;
                case "CR":
                    boolean flag = true;
                    for (int i = 0; i < 4; i++) {
                        if (M[x][i] != R[i]) {
                            flag = false;
                        }
                    }
                    if (flag == true) {
                        C = true;
                    } else {
                        C = false;
                    }
                    break;
                case "BT":
                    if (C == true) {
                        IC[0] = IR[2] - '0';
                        IC[1] = IR[3] - '0';
                    }
                    break;
                case "GD":
                    SI = 1;
                    MOS();
                    break;
                case "PD":
                    SI = 2;
                    MOS();
                    break;
                case "H":
                    SI = 3;
                    MOS();
                    break;
            }

            if (IR[0] == 'H') {
                return;
            }
        }

    }

    static void LOAD() throws IOException {
        int m = 0;
        while (fin.ready()) {
            buffer = fin.readLine();
            if (m < 100) {
                if (buffer.startsWith("$AMJ")) {
                    init();
                    buffer = fin.readLine();
                    while (buffer.charAt(0) != '$') {
                        int idx = 0;
                        if (buffer.charAt(0) != '$') {
                            for (int i = m; i < m + 10; i++) {
                                for (int j = 0; j < 4; j++) {
                                    if (idx < buffer.length()) {
                                        M[i][j] = buffer.charAt(idx++);
                                    }
                                }
                            }
                            m += 10;
                        }
                        buffer = fin.readLine();
                    }
                }
                if (buffer.startsWith("$DTA")) {
                    STARTEXECUTION();
                } else if (buffer.startsWith("$END")) {
                    System.out.println("\nMemory\n");
                    for (int i = 0; i < 100; i++) {
                        for (int j = 0; j < 4; j++) {
                            System.out.print(M[i][j] + " ");
                        }
                        System.out.println();
                    }
                    m = 0;
                }
            } else {
                System.out.println("memory exceeded");
            }
        }
    }

    public static void main(String[] args) {
        try (final BufferedReader br = new BufferedReader(new FileReader("input.txt"));
                final BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt"))) {
            fin = br;
            fout = bw;
            LOAD();

        } catch (IOException e) {
            System.out.println("File not found or error: " + e.getMessage());
        }
        try {
            fin.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}