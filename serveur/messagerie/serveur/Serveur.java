package messagerie.serveur;

import messagerie.serveur.utilisateur.Utilisateur;

import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

/**
 * Classe chargée de démarrer le serveur. Celui-ci a pour objectif d'établir les 
 * communications avec les clients, ainsi que de restaurer les données après redémarrage de celui-ci.
 */
public class Serveur {
	/**
	 * Objet serveur
	 */
	private Server serveur;

	/**
	 * Instance d'application utilisée.
	 */
	private Application application;

	/**
	 * Création d'un objet serveur.
	 */
	public Serveur() {
		System.out.println("Initializing server...");
		if (initialize("serveur.save"))
			System.out.println("Server restored to its previous state!");
		else {
			this.application = Application.getInstance();
			System.out.println("Server initialized!");
		}

		Session.setApplication(this.application);

		this.serveur = new Server("localhost", 4000, "/", null, Session.class);
	}

	/**
	 * Récupérer une précédente instance d'Application si le serveur a déjà été exécuté.
	 * @param fileName Nom du fichier de sauvegarde.
	 * @return Vrai si l'instance d'Application a pu être récupérée. Faux sinon.
	 */
	public boolean initialize(String fileName) {
		ObjectInputStream ois = null;
		File fichier = new File(fileName);

		if (fichier.exists()){
			try {
				ois = new ObjectInputStream(new FileInputStream(fichier));
				this.application = (Application)ois.readObject();
				
				if (ois != null) 
					ois.close();

				return true;

			} catch(IOException e) {
				e.printStackTrace();
			} catch(ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	/**
	 * Enregistrer l'instance d'Application.
	 * @param fileName Fichier de sauvegarde.
	 * @throws IOException Si la sauvegarde n'a pas pu être effectuée.
	 */
	public void enregistrer(String fileName) throws IOException {
		ObjectOutputStream oos = null;

		oos = new ObjectOutputStream(new FileOutputStream(new File(fileName)));
		oos.writeObject(application);

		if (oos != null)
			oos.close();
	}

	/**
	 * Démarre le serveur afin qu'il écoute les clients.
	 * @throws DeploymentException Si le serveur n'a pas pu être démarré.
	 */
	public void start() throws DeploymentException {
		this.serveur.start();
	}

	/**
	 * Arrête le serveur.
	 * @throws DeploymentException Si le serveur n'a pas pu être arrêté.
	 */
	public void stop() throws DeploymentException {
		
		this.serveur.stop();
	}

	public static void main(String[] args) {
		Serveur serveur = null;

		try {
			serveur = new Serveur();
			serveur.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Appuyez sur 'Entrée' pour stopper le serveur...");
			reader.readLine();
		}
		catch (DeploymentException de) {
			de.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (serveur != null) {
					serveur.enregistrer("sauvegarde.save");
					serveur.stop();
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			catch (DeploymentException de) {
				de.printStackTrace();
			}
		}
	}
}
