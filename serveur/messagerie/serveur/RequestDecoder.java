package messagerie.serveur;

import messagerie.serveur.utilisateur.*;
import messagerie.serveur.discussion.*;
import messagerie.serveur.exceptions.*;
import messagerie.serveur.exceptions.DiscussionException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import java.lang.reflect.Method;

import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import java.util.List;
import java.util.ArrayList;
import java.util.*;

import java.io.IOException;

import messagerie.serveur.filtre.*;

/**
 * Cette classe est chargée de décoder les requêtes
 * envoyées par le client au seveur.
 */
public class RequestDecoder {
	/**
	 * Méthodes de la classe pour simplifier l'appel aux différentes méthodes selon l'action
	 * indiquée dans la requête.
	 */
	private final static Method[] methods;

	static {
		Class<RequestDecoder> requestDecoder = RequestDecoder.class;
		methods = requestDecoder.getMethods();
	}

	/**
	 * Session d'où provient la requête
	 */
	private Session session ;

	/**
	 * Encodeur pour les réponses.
	 */
	private ResponseEncoder encodeur;

	/**
	 * Crée un objet RequestDecoder permettant de décoder des requêtes JSON.
	 * @param session Session d'où provient les requêtes.
	 */
	public RequestDecoder(Session session){
		this.session = session ;
		this.encodeur = new ResponseEncoder(session);
	}

	/**
	 * Transforme une chaine de caractère en objet JSON et appelle l'action associée.
	 * @param json Chaine à décoder
	 */
	public void decode(String json){
		JSONParser parser = new JSONParser() ;

		try{
			JSONObject obj =  (JSONObject)parser.parse(json);
			String action = obj.get("action").toString();
			JSONObject content = (JSONObject)obj.get("contenu");

			Method requested = RequestDecoder.class.getMethod(action, JSONObject.class);

			requested.invoke(this, content);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connexion d'un utilisateur.
	 * @param content Requête reçue par le serveur.
	 */
	public void connexion(JSONObject content) {
		try{
			String pseudo = ((String)content.get("pseudonyme")).toUpperCase();
			String mdp = (String)content.get("mot_de_passe");
			UtilisateurHumain utilisateur = (UtilisateurHumain)Session.getApplication().getUtilisateur(pseudo);
			if(utilisateur.verifieMotDePasse(mdp)){
				this.session.setUtilisateur(utilisateur);
				utilisateur.setSession(this.session);
				this.session.envoyerMessage(
					this.encodeur.connexionReponse(true)
				);
			}
			else
				this.session.envoyerMessage(
					this.encodeur.connexionReponse(false)
				);
		}
		catch (UtilisateurException ue) {
			System.err.println(ue.getMessage());

			try {
				this.session.envoyerMessage(
					this.encodeur.connexionReponse(false)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
		catch (Exception pe) {
			pe.printStackTrace();
		}
	}

	/**
	 * Création d'un utilisateur.
	 * @param content Requête reçue par le serveur.
	 */
	public void creer_utilisateur(JSONObject content) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH);
		try{
			UtilisateurHumain utilisateur = new UtilisateurHumain(
												((String)content.get("pseudonyme")).toUpperCase(),
												(String)content.get("nom"),
												(String)content.get("prenom"),
												(String)content.get("mot_de_passe"),
												(String)content.get("adresse_mel"),
												format.parse((String)content.get("date_naissance"))
											);

			Session.getApplication().ajouterUtilisateur(utilisateur);

			this.session.envoyerMessage(
				this.encodeur.creerUtilisateurReponse(true, utilisateur)
			);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());

			try {
				this.session.envoyerMessage(
					this.encodeur.creerUtilisateurReponse(false, null)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Création d'une discussion.
	 * @param content Requête reçue par le serveur.
	 */
	public void creer_discussion(JSONObject content) {
		Discussion discussion = null;
		List<Utilisateur> utilisateurs = new ArrayList<>();

		try {
			JSONArray pseudonymes = (JSONArray)content.get("utilisateurs");
			for (Object p : pseudonymes)
				utilisateurs.add(Session.getApplication().getUtilisateur((String)p));

			if (this.session.getUtilisateur() != null)
				utilisateurs.add(this.session.getUtilisateur());
			else
				throw new DiscussionException("Impossible de créer la discussion. L'utilisateur souhaitant la créer n'est pas connecté.");
			// on prend un users et on check toute ces disc si 1 existe deja on la renvoie
			for(Discussion disc : utilisateurs.get(0).getDiscussions()){
				List<Utilisateur> usersDisc = disc.getUtilisateurs();
				if(utilisateurs.containsAll(usersDisc) && usersDisc.containsAll(utilisateurs) ){
					discussion = disc ;
				}
			}
			if(discussion == null){
				discussion = new DiscussionTexte(utilisateurs);
				Session.getApplication().ajouterDiscussion(discussion);
			}
			this.session.envoyerMessage(
				this.encodeur.creerDiscussionReponse(true, discussion)
			);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());

			try {
				this.session.envoyerMessage(
					this.encodeur.creerDiscussionReponse(false, discussion)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	/**
	 * Récuperer la discussion
	 * @param content Requête reçue par le serveur.
	 */

	public void get_discussion(JSONObject content) {
		try {
			int id = Integer.parseInt((String)content.get("id_discussion"));
			Discussion discussion = Session.getApplication().getDiscussion(id);
			this.session.envoyerMessage(
				this.encodeur.getDiscussionReponse(true, discussion)
			);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());

			try {
				this.session.envoyerMessage(
					this.encodeur.getDiscussionReponse(false, null)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}




	/**
	 * Envoi d'un message aux utilisateurs d'une discussion.
	 * @param content Requête reçue par le serveur.
	 */
	public void envoyer_message(JSONObject content) {
		// On ne peut envoyer le msg que si la session a un utilisateur
		try{
			if (this.session.getUtilisateur() == null)
				throw new UtilisateurException("Impossible d'envoyer un message pour un utilisateur non connecté.");

			int id = Integer.parseInt((String)content.get("id_discussion"));
			String texteMessage = (String)content.get("message");
			DiscussionTexte discussion = ((DiscussionTexte)Session.getApplication().getDiscussion(id));

			Message message = new Message(this.session.getUtilisateur(), texteMessage, discussion) ;

			if (!discussion.possedeUtilisateur(this.session.getUtilisateur()))
				throw new DiscussionException("L'utilisateur n'est pas dans la conversation. Impossible d'envoyer un message.");

			this.session.envoyerMessage(
				this.encodeur.envoyerMessageReponse(true)
			);
			
			if (this.session.getUtilisateur() instanceof UtilisateurHumain)
				((UtilisateurHumain)this.session.getUtilisateur()).envoyerMessage(this.encodeur.encoderMessage(message, this.session.getUtilisateur()));

			envoieMessageUtilisateurs(discussion, message);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());

			try {
				this.session.envoyerMessage(
					this.encodeur.envoyerMessageReponse(false)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private void envoieMessageUtilisateurs(DiscussionTexte discussion, Message message) {
		discussion.addMessage(message);

		for (Utilisateur u : discussion.getUtilisateurs())
				if (u.equals(message.getUtilisateur()))
					continue;
				else if (u instanceof UtilisateurHumain)
					((UtilisateurHumain)u).envoyerMessage(this.encodeur.encoderMessage(message, this.session.getUtilisateur()));
				else if (u instanceof UtilisateurIA)
					try {
						envoieMessageUtilisateurs(discussion, ((UtilisateurIA)u).repondre(message));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
	}

	/**
	 * Envoi de la liste des utilisateurs au client.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_discussions(JSONObject content) {
		try {
			this.session.envoyerMessage(
				this.encodeur.getDiscussionsReponse(true)
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Envoi de la liste des discussions au client.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_utilisateurs(JSONObject content) {
		try {
			this.session.envoyerMessage(
				this.encodeur.getUtilisateursReponse()
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Modification du profil d'un utilisateur.
	 * @param content Requête reçue par le serveur.
	 */
	public void modifier_profil(JSONObject content) {
		//DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

		try {
			Utilisateur utilisateur = this.session.getUtilisateur();
			if (!(utilisateur instanceof UtilisateurHumain))
				throw new UtilisateurException("Seul un UtilisateurHumain peut être modifié.");

			UtilisateurHumain utilHumain = (UtilisateurHumain)utilisateur;
			utilHumain.setMotDePasse((String)content.get("mot_de_passe"))
					  .setAdresseMel((String)content.get("adresse_mel"))
					  //.setDateNaissance(format.parse((String)content.get("date_naissance")))
					  .setNom((String)content.get("nom"))
					  .setPrenom((String)content.get("prenom"));

			this.session.envoyerMessage(
				this.encodeur.modifierProfilReponse(true)
			);
		}
		catch (UtilisateurException ue) {
			System.err.println(ue.getMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Envoi du profil d'un utilisateur.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_profil(JSONObject content) {
		try {
			this.session.envoyerMessage(
				this.encodeur.getProfilReponse(true)
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Filtrer un mot pour l'utilisateur correspondant à la session active.
	 * @param content Requête reçue par le serveur.
	 */
	public void add_filtre_mot(JSONObject content) {
		if (this.session.getUtilisateur() != null){
			try{
				String mdp = (String)content.get("mot_de_passe_parental");
				String mot = (String)content.get("mot");

				UtilisateurHumain utilisateur = (UtilisateurHumain)this.session.getUtilisateur();
				if (utilisateur.verifieMotDePasseParental(mdp)){
					utilisateur.ajouterFiltre(new FiltreMot(mot));
					try {
						this.session.envoyerMessage(
							this.encodeur.addFiltreMotReponse(true)
						);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}else{
					try {
						this.session.envoyerMessage(
							this.encodeur.addFiltreMotReponse(false)
						);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			} catch (Exception pe) {
				pe.printStackTrace();
			}
		}else{
			try {
				this.session.envoyerMessage(
					this.encodeur.addFiltreMotReponse(false)
				);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Filtrer un utilisateur pour l'utilisateur correspondant à la session active.
	 * @param content Requête reçue par le serveur.
	 */
	public void add_filtre_utilisateur(JSONObject content) {
		try {
			String mdp = (String)content.get("mot_de_passe_parental");
			String nom = (String)content.get("utilisateur");

			UtilisateurHumain utilisateur = (UtilisateurHumain)this.session.getUtilisateur();
			if (utilisateur.verifieMotDePasseParental(mdp)){
				Utilisateur utilisateurBanni = Session.getApplication().getUtilisateur(nom);
				utilisateur.ajouterFiltre(new FiltreUtilisateur(utilisateurBanni));
				try {
					this.session.envoyerMessage(
						this.encodeur.addFiltreUtilisateurReponse(true)
					);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}else{
				try {
					this.session.envoyerMessage(
						this.encodeur.addFiltreUtilisateurReponse(false)
						);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
		catch (Exception pe) {
			pe.printStackTrace();
		}
	}

	/**
	 * Activer le contrôle parental.
	 * @param content Requête reçue par le serveur.
	 */
	public void set_controle_parental(JSONObject content) {
		try {
			String mdp = (String)content.get("mot_de_passe_parental");

			UtilisateurHumain utilisateur = (UtilisateurHumain)this.session.getUtilisateur();
			if (utilisateur.verifieMotDePasseParental(null)) {
				utilisateur.setMotDePasseParental(true, mdp);
				try {
					this.session.envoyerMessage(
						this.encodeur.setControleParentalReponse(true)
					);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}else{
				try {
					this.session.envoyerMessage(
						this.encodeur.setControleParentalReponse(false)
					);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}

		}
		catch (Exception pe) {
			pe.printStackTrace();
		}
	}

	/**
	 * Désactiver le contrôle parental.
	 * @param content Requête reçue par le serveur.
	 */
	public void desactiver_controle_parental(JSONObject content) {
		try {
			String mdp = (String)content.get("mot_de_passe_parental");

			UtilisateurHumain utilisateur = (UtilisateurHumain)this.session.getUtilisateur();
			if (utilisateur.verifieMotDePasseParental(mdp)){
				utilisateur.setMotDePasseParental(false, null);
				try {
					this.session.envoyerMessage(
						this.encodeur.desactiverControleParentalReponse(true)
					);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			else{
				try {
					this.session.envoyerMessage(
						this.encodeur.desactiverControleParentalReponse(false)
					);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}

			}
		}
		catch (Exception pe) {
			pe.printStackTrace();
		}
	}
	/**
	 * get filtres mot.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_filtres_mot(JSONObject content){
		try {
			this.session.envoyerMessage(
				this.encodeur.getFiltreMotReponse(true)
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * get filtres utilisateur.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_filtres_utilisateur(JSONObject content){
		try {
			this.session.envoyerMessage(
				this.encodeur.getFiltreUtilisateurReponse(true)
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	/**
	 * get controle parental.
	 * @param content Requête reçue par le serveur.
	 */
	public void get_controle_parental(JSONObject content){
		try {
			this.session.envoyerMessage(
				this.encodeur.getControleParentalReponse(true)
			);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) {
		RequestDecoder rd = new RequestDecoder(null);
		rd.decode("{\"action\":\"connexion\", \"contenu\":{\"pseudonyme\" : \"TheLegend27\",\"mot_de_passe\" : \"motDePasse\"}}");
		rd.decode("{\"action\":\"creer\", \"contenu\":{\"pseudonyme\" : \"Orionis\",\"mot_de_passe\" : \"motDePasse\",\"nom\" : \"Zerhouni\",\"prenom\" : \"Youssef\",\"adresse_mel\" : \"youssef.zerhouni_abdou@insa-rouen.fr\",\"date_naissance\" : \"28/01/1996\"}}");
	}
}
