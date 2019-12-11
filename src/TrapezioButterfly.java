import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import mpi.*;

/**
 * TrapezioButterfly
 */
public class TrapezioButterfly {

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
        double soma[] = new double[1];
        
        MPI.Init(args); // inicia mpi
        double tempo_inicial = System.currentTimeMillis(); // tempo inicial
        proc_atual = MPI.COMM_WORLD.Rank(); // id do processo atual
        num_proc = MPI.COMM_WORLD.Size(); // numero total de processos

        if (proc_atual == 0){
            // processo mestre
            //System.out.println("Processo mestre iniciado!");

            // parametros da integral
            param[0] = Double.parseDouble(args[3]); // a processo
            param[1] = Double.parseDouble(args[4]); // b processo
            param[2] = Double.parseDouble(args[5]); // h processo

            // envia as partes para os escravos
            for (int i=1; i < num_proc; i++){
                MPI.COMM_WORLD.Send(param, 0, 3, MPI.DOUBLE, i, 5);                
            }
        }
        else {
            // recebe dados do mestre
            MPI.COMM_WORLD.Recv(param, 0 , 3, MPI.DOUBLE, 0, 5);
        }

        double tam_parte = (param[1]-param[0])/num_proc;
        double inicio = (proc_atual*tam_parte)+param[0]; // a processo
        double fim = ((proc_atual+1)*tam_parte)+param[0]; // b processo
        double intervalo = param[2]; // h processo

        // processa a parte do processo
        double somatorio = integrar(inicio, fim, intervalo);

        int metade = num_proc;
        do {
            metade = metade/2;
            soma[0] = somatorio;
            
            if (proc_atual >= metade){
                MPI.COMM_WORLD.Send(soma, 0, 1, MPI.DOUBLE, (proc_atual-metade), 10);
            }
            else {
                MPI.COMM_WORLD.Recv(soma, 0 , 1, MPI.DOUBLE, (proc_atual+metade), 10);
                //System.out.println("processo "+proc_atual+": soma parcial recebida do processo "+(proc_atual+metade)+".");
                somatorio = soma[0] + somatorio;
            }

        } while (proc_atual < metade && metade > 1);
        
        if (proc_atual == 0){
            // obtem o tempo de execução
            double tempo_final = System.currentTimeMillis();
            double tempo_total = (tempo_final - tempo_inicial)/1000;

            // formata o número apresentado
            NumberFormat double_form = new DecimalFormat("0.########", new DecimalFormatSymbols(Locale.US));

            System.out.println(
                "{\n"+
                "\t\"titulo\": \""+args[6]+"\",\n"+
                    "\t\"integral\": \"f(x) = 5x^3 + 3x^2 + 4x + 20\",\n"+
                    "\t\"limite_inferior\": "+param[0]+",\n"+
                    "\t\"limite_superior\": "+param[1]+",\n"+
                    "\t\"precisao\": "+param[2]+",\n"+
                    "\t\"resultado\": "+double_form.format(somatorio)+",\n"+
                    "\t\"tempo\": "+double_form.format(tempo_total)+",\n"+
                    "\t\"num_processos\": "+args[1]+"\n"+
                "}"
            );
        }

        MPI.Finalize();  //finaliza a parte distribuída
    }
}