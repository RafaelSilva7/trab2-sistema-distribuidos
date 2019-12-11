import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import mpi.*;

/**
 * TrapezioBalanceamento
 */
public class TrapezioBalanceamento {
    static final int KILLTAG = 2;

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
        int num_proc, proc_atual;
        double param[] = new double[3];
        double soma_parcial[] = new double[1];
        
        MPI.Init(args); // inicia mpi
        double tempo_inicial = System.currentTimeMillis(); // tempo inicial
        
        proc_atual = MPI.COMM_WORLD.Rank(); // id do processo atual
        num_proc = MPI.COMM_WORLD.Size(); // numero total de processos

        // processo mestre
        if (proc_atual == 0){
            // processo mestre
            //System.out.println("Processo mestre iniciado!");

            // parametros da integral
            double a_total = Double.parseDouble(args[3]); // a da intergral
            double b_total = Double.parseDouble(args[4]); // b da intergral
            double h = Double.parseDouble(args[5]); // h da intergral

            int num_partes = 32;
            double v_inicial[] = new double[num_partes];
            double v_final[] = new double[num_partes];
            
            // calcula os intervalos de cada parte
            double tam_partes = (b_total-a_total)/num_partes;
            for (int i=0; i < num_partes; i++){
                v_inicial[i] = (i*tam_partes)+a_total; // a da parte i
                v_final[i] = ((i+1)*tam_partes)+a_total; // b da parte i
            }

            // encaminha as partes para os processos
            int enviado = 0;
            for (int i=1; i < num_proc; i++) {
                param[0] = v_inicial[enviado];
                param[1] = v_final[enviado];
                param[2] = h;

                MPI.COMM_WORLD.Send(param, 0, 3, MPI.DOUBLE, i, 5);
                //System.out.println("Enviado parte para o processo: "+i+": "+soma_parcial[0]+" : param ["+param[0]+","+param[1]+","+param[2]+"]");
                enviado++;  
                //System.out.println("Resta: "+(num_partes-enviado));
            }

            // encaminha todas as partes conforme a disponibilidade
            double soma_total = 0;
            while (enviado < num_partes){
                // recebe de qualquer processo
                Status status;
                status = MPI.COMM_WORLD.Recv(soma_parcial, 0 , 1, MPI.DOUBLE, MPI.ANY_SOURCE, 10);
                soma_total += soma_parcial[0];
                //System.out.println("Recebido soma parcial de processo: "+status.source);
                
                // envia para o processo ocioso
                param[0] = v_inicial[enviado];
                param[1] = v_final[enviado];
                param[2] = h;
                MPI.COMM_WORLD.Send(param, 0, 3, MPI.DOUBLE, status.source, 5);
                //System.out.println("Enviado parte para o processo: "+status.source+": "+soma_parcial[0]+" : param ["+param[0]+","+param[1]+","+param[2]+"]");
                enviado++;
                //System.out.println("Resta: "+(num_partes-enviado));
            }

            // receba as ultimas partes envidadas
            for (int i=1; i < num_proc; i++){
                MPI.COMM_WORLD.Recv(soma_parcial, 0 , 1, MPI.DOUBLE,i, 10);
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

            for(int i=1; i < num_proc; i++){
                MPI.COMM_WORLD.Send(param, 0, 3, MPI.DOUBLE, i, KILLTAG);
                //System.out.println("Envio de kill para proc: " + i);    
            }
        }

        // processom escravo
        else {
            for (;;) {
                // recebe o inicio e o fim do mestre
                Status status = MPI.COMM_WORLD.Recv(param, 0, 3, MPI.DOUBLE, 0, MPI.ANY_TAG);
                if (status.tag == KILLTAG){
                    break;
                }

                soma_parcial[0] = integrar(param[0], param[1], param[2]);

                MPI.COMM_WORLD.Send(soma_parcial, 0, 1, MPI.DOUBLE, 0, 10);
            }
        }

        MPI.Finalize();  //finaliza a parte distribuída
    }
}