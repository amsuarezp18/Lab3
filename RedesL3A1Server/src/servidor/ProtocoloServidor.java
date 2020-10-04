package servidor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

public class ProtocoloServidor implements Runnable{
	
	//Rutas de archivos
	private static String ARCHIVO_1 = "data/archivos/video1.mp4";
	private static String ARCHIVO_2 = "data/archivos/test2.bin";
	
	//Constantes
	private static int TAM_PAQUETE = 1024;

	//Atributos
	private Socket sc = null;
	private int idP;
	private long time_start, time_end, time;
	private static File fileLog;
	private int archivo;


	/*
	 * Constructor del protocolo del servidor
	 * @param: csP socket designado
	 * @param: idP Numero de thread que atiende
	 */
	public ProtocoloServidor (Socket csP, int idP, int numArchivo, File archivoLog) {
		
		fileLog = archivoLog;
		sc = csP;
		this.idP=idP;
		archivo = numArchivo;
		this.run();

	}

	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo . 
	 * - Es el ÃƒÂºnico metodo permitido para escribir en el log.
	 */
	private void escribirLog(String pCadena) {
		synchronized(fileLog)
		{
			try {
				FileWriter fw = new FileWriter(fileLog,true);
				fw.append(pCadena + "\n");
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * Método que genera hash para verificar inttegridad
	 * @param ruta ruta del archivo
	 * @param md MessageDigest que iene el algoritmo
	 * @return String hexadecimal con hash de archivo
	 * @throws IOException si falla lectura de archivo
	 */
	private String checksum(String ruta, MessageDigest md ) throws IOException{
		try (DigestInputStream dis = new DigestInputStream(new FileInputStream(ruta), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
	}
	
	@Override
	public void run() {

		try {
			
			//System.out.println("bandera 1");
			//RECUPERA EL ARCHIVO A ENVIAR
			File file;
			if(archivo == 1) {
				file = new File(ARCHIVO_1);
			}
			else {
				file = new File(ARCHIVO_2);
			}
			//CREA BUFFER Y CANALES DE COMUNICACION EN SOCKET
			byte[] archivoBytes = new byte[(int) file.length()];
			FileInputStream fi = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fi);
			//System.out.println("bandera 2");
			bis.read(archivoBytes, 0, archivoBytes.length);
			//System.out.println("bandera 3");
			OutputStream os = sc.getOutputStream();
			
			//GENERA HASH DEL ARCHIVO PARA COMPROBACION
			
			//VERSION PRUEBA
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			//System.out.println("bandera 4");
			//String hexa = checksum(file.getPath(), md);
			//System.out.println("bandera 5");
			//System.out.println("Hexa serv " + hexa);
			DataOutputStream dos = new DataOutputStream(sc.getOutputStream());
			//dos.writeUTF(hexa);
			dos.writeInt((int)file.length());
			//System.out.println("bandera 6");
			//NOTIFICA ENVIO DE ARCHIVO Y COMIENZA PROCESO
			System.out.println("Enviando "+ file.getName() + " tamano: " + archivoBytes.length + " Bytes");
			
			int enviados = 0;
			time_start = System.currentTimeMillis();
			for(int i = 0; i <= file.length() - (file.length() % TAM_PAQUETE) ; i+=TAM_PAQUETE ) {
				if(i == file.length() - (file.length() % TAM_PAQUETE)) {
					os.write(archivoBytes, i, (int)file.length() % TAM_PAQUETE);
				}
				else {
					os.write(archivoBytes, i, TAM_PAQUETE);
				}
				enviados++;
				System.out.println("Enviando paquete " + enviados + "/" + (int)(file.length() / TAM_PAQUETE));
			}
			
			
			//NOTIFICA TERMINACION DE ENVIO Y RECEPCION DEL ARCHIVO POR PARTE EL CLIENTE
			DataInputStream dis = new DataInputStream(sc.getInputStream());
			if(dis.readByte() == 1) {
				time_end = System.currentTimeMillis();
				time = time_end - time_start;
				String cadena = "Entrega de archivo a cliente " + idP + " fue exitosa. Tomó " + time / 1000 + " segundos";
				escribirLog(cadena);
				cadena = "Paquetes enviados " + enviados + " se enviaron " + (int) file.length() + " Bytes";
				escribirLog(cadena);
				cadena = "Paquetes recibidos " + enviados + " se recibieron " + (int) file.length() + " Bytes";
				escribirLog(cadena);
				System.out.println("Envío de archivo terminado. Cliente ya lo recibió.");
			}
			dis.close();
			bis.close();
			os.close();
			sc.close();
		}
		catch(Exception e) {
			System.out.println("Error en proceso de envío... " + e.getMessage());
		}
		
	}



}
