import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import mpi.*;

/**
 * Trapezio
 */
public class Trapezio {

    static double function(double x){
        return 5*Math.pow(x, 3.0) + 3*Math.pow(x, 2.0) + 4*x + 20; 
    }

    static double integrar(double a, double b, double h){
        int n_partes = (int) ((b - a) / h);
        double soma = h*0.5*(function(a)+function(b));
        for (int i = 1; i < n_partes; i++){
            soma += h * function(h*i+a);
        }
        return soma;
    }

    public static void main(String[] args) {
        double param[] = new double[3];
        double soma_parcial[] = new double[1]; 

        int n_proc, proc_atual;

        MPI.Init(args); // inicia mpi
        proc_atual = MPI.COMM_WORLD.Rank(); // id do processo atual
        n_proc = MPI.COMM_WORLD.Size(); // numero total de processos

        // processo mestre
        if (proc_atual == 0){
            double tempo_inicial = System.currentTimeMillis();
            //System.out.println("Processo mestre iniciado!");

            double a_total = Double.parseDouble(args[3]);
            double b_total = Double.parseDouble(args[4]);
            double h = Double.parseDouble(args[5]);

            // envia as partes para os escravos
            double h_mestre = (b_total-a_total)/n_proc;
            for (int i=1; i < n_proc; i++){
                param[0] = (i*h_mestre)+a_total; // a processo
                param[1] = ((i+1)*h_mestre)+a_total; // b processo
                param[2] = h; // h processo

                MPI.COMM_WORLD.Send(param, 0, 3, MPI.DOUBLE, i, 5);                
            }

            // processa a parte do mestre
            double soma_total = integrar(a_total, a_total+h_mestre, h);
            //System.out.println("processo 0: "+soma_total+" : param ["+a_total+","+(a_total+h_mestre)+","+h+"]");
            
            // recebe as somas parciais dos escravos
            for (int i=1; i < n_proc; i++){
                MPI.COMM_WORLD.Recv(soma_parcial, 0 , 1, MPI.DOUBLE, i, 10);
                //System.out.println("mestre: soma parcial recebida do processo "+i+".");
                soma_total += soma_parcial[0];
            }

            // obtem o tempo de execução
            double tempo_final = System.currentTimeMillis();
            double tempo_total = (tempo_final - tempo_inicial)/1000;

            // formata o número apresentado
            NumberFormat double_form = new DecimalFormat("0.#####", new DecimalFormatSymbols(Locale.US));

            System.out.println(
                "{\n"+
                "\t\"titulo\": \""+args[6]+"\",\n"+
                    "\t\"integral\": \"f(x) = 5x^3 + 3x^2 + 4x + 20\",\n"+
                    "\t\"limite_inferior\": "+a_total+",\n"+
                    "\t\"limite_superior\": "+b_total+",\n"+
                    "\t\"precisao\": "+h+",\n"+
                    "\t\"resultado\": "+double_form.format(soma_total)+",\n"+
                    "\t\"tempo\": "+double_form.format(tempo_total)+",\n"+
                    "\t\"num_processos\": "+args[1]+"\n"+
                "}"
            );
        }

        // processo escravo
        else {
            // recebe dados do mestre
            MPI.COMM_WORLD.Recv(param, 0 , 3, MPI.DOUBLE, 0, 5);

            // processa a parte do escravo
            soma_parcial[0] = integrar(param[0], param[1], param[2]);
            //System.out.println("processo "+proc_atual+": "+soma_parcial[0]+" : param ["+param[0]+","+param[1]+","+param[2]+"]");
            
            MPI.COMM_WORLD.Send(soma_parcial, 0, 1, MPI.DOUBLE, 0, 10);
        }

        MPI.Finalize();  //finaliza a parte distribuída
    }
}