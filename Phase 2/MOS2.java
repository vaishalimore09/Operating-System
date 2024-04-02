import java.io.*;
import java.util.*;

class PCB {
    int JID;
    int TTL;
    int TLL;
    int TTC;
    int LLC;
}

public class MOS2 {
    static char[][] M = new char[300][4];
    static char IR[] = new char[4];
    static String error_message_coding[] = { "No Error", "Out of Data", "Line Limit Exceeded", "Time Limit Exceeded",
            "Operation Code Error", "Operand Error", "Invalid Page Fault" };
    static char R[] = new char[4];
    static ArrayList<Integer> frameNumbers = new ArrayList<Integer>();
    static Random rand = new Random();
    static String buffer, Examine;
    static BufferedReader fin;
    static BufferedWriter fout;
    static int IC, SI, PI, TI, PTR, VA, RA;
    static boolean isPFHandeled, isExceed, C;
    static PCB PCB;

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 30; i++) {
            frameNumbers.add(i);
        }
        final BufferedReader br = new BufferedReader(new FileReader("input.txt"));
        final BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt"));
        fin = br;
        fout = bw;
        LOAD();
        fin.close();
        fout.close();
    }

    static void LOAD() throws IOException {
        while (fin.ready()) {
            buffer = fin.readLine();
            if (buffer.startsWith("$AMJ")) {
                init();
                if (buffer.length() >= 16) {
                    String jid = buffer.substring(4, 8);
                    String ttl = buffer.substring(8, 12);
                    String tll = buffer.substring(12, 16);
                    // load PCB
                    PCB.JID = Integer.parseInt(jid);
                    PCB.TTL = Integer.parseInt(ttl);
                    PCB.TLL = Integer.parseInt(tll);
                    PCB.LLC = 0;
                    PCB.TTC = 0;
                    // initialize PTR
                    PTR = ALLOCATE();
                    // initialize page table with '*'
                    for (int i = PTR * 10; i < PTR * 10 + 10; i++) {
                        for (int j = 0; j < 4; j++) {
                            M[i][j] = '*';
                        }
                    }
                } else {
                    System.out.println("provide correct job");
                }
            } else if (buffer.startsWith("$DTA")) {
                // start execution of job
                STARTEXECUTION();
            } else if (buffer.startsWith("$END")) {
                // next job if available
                continue;
            } else {
                // new frame to store program cards
                int frame = ALLOCATE();
                int ptr = PTR;
                while (M[ptr][0] != '*') {
                    ptr++;
                }

                // page table entry
                M[ptr][0] = (char) (1 + '0');
                M[ptr][1] = ' ';
                M[ptr][2] = (char) (frame / 10 + '0');
                M[ptr][3] = (char) (frame % 10 + '0');
                int k = 0;

                // load program cards in memory
                for (int i = frame * 10; i < frame * 10 + 10 && k < buffer.length(); i++) {
                    for (int j = 0; j < 4 && k < buffer.length(); j++) {
                        M[i][j] = buffer.charAt(k++);
                    }
                }
            }
        }

    }

    static void init() {
        for (int i = 0; i < 4; i++) {
            IR[i] = ' ';
            R[i] = ' ';
        }
        IC = 0;
        C = false;
        SI = 0;
        PI = 0;
        TI = 0;
        for (int i = 0; i < 300; i++) {
            for (int j = 0; j < 4; j++) {
                M[i][j] = '-';
            }
        }
        for (int i = 0; i < 30; i++) {
            if (!frameNumbers.contains(i)) {
                frameNumbers.add(i);
            }
        }
        PTR = -1;
        PCB = new PCB();
    }

    static void STARTEXECUTION() throws IOException {
        IC = 0;
        EXECUTEUSERPROGRAM();
    }

    static void EXECUTEUSERPROGRAM() throws IOException {
        while (true) {
            // map IC table to fetch instruction
            RA = ADDRESS_MAP(IC);

            // load instruction
            for (int i = 0; i < 4; i++) {
                IR[i] = M[RA][i];
            }
            IC++;

            if (IR[0] != 'H') {
                if (!Character.isDigit(IR[2]) || !Character.isDigit(IR[3])) {
                    PI = 2; // operand error if not digit
                } else {
                    int frame = ((IR[2] - '0') * 10 + IR[3] - '0');
                    RA = ADDRESS_MAP(frame); // if operand is number get real address for operand
                }
            }

            if ((PI != 0 && TI != 0) || PI != 0) {
                MOS(); // handle error(page fault, operand, time limit + operand)
                if (!isPFHandeled || PI == 2) {
                    return; // if invalid page fault or operand error --> return
                }
                int frame = ((IR[2] - '0') * 10 + IR[3] - '0');
                RA = ADDRESS_MAP(frame); // after handling valid page map real address
                isPFHandeled = false;
            }

            // Operation code --> Examine
            if (IR[0] == 'H') {
                Examine = IR[0] + "";
            } else {
                Examine = IR[0] + "" + IR[1];
            }

            switch (Examine) {
                case "LR":
                    for (int i = 0; i < 4; i++) {
                        R[i] = M[RA][i]; // Memory --> register
                    }
                    break;
                case "SR":
                    for (int i = 0; i < 4; i++) {
                        M[RA][i] = R[i]; // register --> memory
                    }
                    break;
                case "CR":
                    // compare memory and register
                    for (int i = 0; i < 4; i++) {
                        if (M[RA][i] != R[i]) {
                            C = false;
                            break;
                        }
                    }
                    C = true;
                    break;
                case "BT":
                    if (C == true) {
                        int address = (IR[2] - '0') * 10 + (IR[3] - '0');
                        IC = address;
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
                    return;
                default:
                    PI = 1;
                    break;
            }

            if (isExceed) {
                isExceed = false; // return if (LL exceeded, (out of data--find alt))
                return;
            }

            if (TI != 0 || PI != 0) {
                MOS(); // Time Limit, operation code
                TI = 0;
                PI = 0;
                return;
            }
            SIMULATION();
        }

    }

    static int ADDRESS_MAP(int VA) {
        if (VA >= 0 && VA <= 99) { // valid operand (0 to 99)
            int add = PTR * 10 + VA / 10; // point to page table
            if (M[add][0] == '*') {
                PI = 3; // page fault
                return -1;
            }
            RA = ((M[add][2] - '0') * 10 + (M[add][3] - '0')) * 10 + VA % 10; // real address of frame no. in page table
            return RA;
        } else {
            PI = 2; // invalid operand
            return -1;
        }
    }

    static void SIMULATION() throws IOException {
        PCB.TTC += 1; // inc TTC
        if (PCB.TTC == PCB.TTL) {
            TI = 2; // Time limit exceeded
        }
    }

    static int ALLOCATE() {
        int x = frameNumbers.get(rand.nextInt(frameNumbers.size()));
        frameNumbers.remove(frameNumbers.indexOf(x));
        return x; // allocate new frame number
    }

    static void MOS() throws IOException {

        if (TI == 0 && SI == 1) {
            READ(); // GD
        } else if (TI == 0 && SI == 2) {
            WRITE(); // PD
        } else if (TI == 0 && SI == 3) {
            PCB.TTC++; // H (directly return(find alt))
            TERMINATE(0);
        } else if (TI == 2 && SI == 1) {
            TERMINATE(3); // time limit + GD
        } else if (TI == 2 && SI == 2) {
            WRITE(); // time limit + PD (write and terminate)
            isExceed = true;
            PCB.TTC++; // directly terminate
            TERMINATE(3);
        } else if (TI == 2 && SI == 3) {
            PCB.TTC++; // time limit + H (directly return(find alt))
            TERMINATE(0);
        }

        if (TI == 0 && PI == 1) {
            TERMINATE(4); // operation code
        } else if (TI == 0 && PI == 2) {
            TERMINATE(5); // operand
        } else if (TI == 0 && PI == 3) {
            handlevalidPF(); // page fault
            PI = 0;
        } else if (TI == 2 && PI == 1) {
            TERMINATE(3, 4);
        } else if (TI == 2 && PI == 2) {
            TERMINATE(3, 5);
        } else if (TI == 2 && PI == 3) {
            TERMINATE(3);
        } else if (TI == 2 && SI == 0) {
            TERMINATE(3);
        }
        SI = 0;
    }

    static void READ() throws IOException {
        fin.mark(1024);

        buffer = fin.readLine();
        if (buffer != null) {
            if (buffer.startsWith("$END")) { // out of data
                fin.reset();
                isExceed = true; // find alt
                TERMINATE(1);
                return;
            }
        }

        int frame = ALLOCATE(); // allocate new frame
        updatePageTable(frame);
        // store new data card
        int k = 0;
        for (int i = frame * 10; i < frame * 10 + 10 && k < buffer.length(); i++) {
            for (int j = 0; j < 4 && k < buffer.length(); j++) {
                M[i][j] = buffer.charAt(k++);
            }
        }
        isPFHandeled = true;
        SI = 0;
    }

    static void WRITE() throws IOException {
        PCB.LLC++; // inc LLC
        if (PCB.LLC > PCB.TLL) {
            isExceed = true; // line limit exceeded (return)
            PCB.LLC--;
            TERMINATE(2);
            return;
        }

        // write M --> file
        for (int i = RA; i < RA + 10; i++) {
            for (int j = 0; j < 4; j++) {
                fout.write(M[i][j]);
            }
        }
        fout.write("\n");
    }

    static void printMemory() {
        System.out.println("Main Memory");
        for (int i = 0; i < 300; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < 4; j++) {
                System.out.print(M[i][j] + " ");
            }
            System.out.println();
        }
    }

    static void jobInfo() throws IOException {
        fout.write("\nIC      : " + IC);
        fout.write("\nIR      : ");
        for (int i = 0; i < 4; i++) {
            fout.write(IR[i] + " ");
        }
        fout.write("\nTTL     : " + PCB.TTL);
        fout.write("\nTTC     : " + PCB.TTC);
        fout.write("\nTLL     : " + PCB.TLL);
        fout.write("\nLLC     : " + PCB.LLC);
        fout.write("\n\n");
    }

    static void TERMINATE(int EM) throws IOException {
        fout.write("\nJOB ID  : " + PCB.JID);
        fout.write("\n  " + error_message_coding[EM]);
        jobInfo();
        printMemory();
        return;
    }

    static void TERMINATE(int TI, int PI) throws IOException {
        fout.write("\nJOB ID  : " + PCB.JID);
        fout.write("\n  " + error_message_coding[TI] + " And " + error_message_coding[PI]);
        jobInfo();
        printMemory();
        return;
    }

    static void updatePageTable(int frame) {
        int ptr = PTR * 10 + (IR[2] - '0');
        // update page table
        M[ptr][0] = (char) (1 + '0');
        M[ptr][1] = ' ';
        M[ptr][2] = (char) (frame / 10 + '0');
        M[ptr][3] = (char) (frame % 10 + '0');
    }

    static void handlevalidPF() throws IOException {
        // handle valid page fault
        if ((IR[0] == 'G' && IR[1] == 'D') || (IR[0] == 'S' && IR[1] == 'R')) {
            int frame = ALLOCATE(); // allocate new frame
            updatePageTable(frame);
            isPFHandeled = true;
        } else {
            isPFHandeled = false;
            TERMINATE(6);
        }
    }
}