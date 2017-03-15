package messagerie.serveur.utilisateur;

import messagerie.serveur.filtre.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Date;

public class UtilisateurHumain extends Utilisateur {
	private static String photoDefaut = "defaut.png";

	private String motDePasse;
	private String adresseMel;
	private Date dateNaissance;
	private String motDePasseParental;
	private String photo;

	private Set<IFiltre> filtres;

	public UtilisateurHumain(String pseudonyme, String nom, String prenom,
							 String motDePasse, String adresseMel,
							 Date dateNaissance) {
		super(pseudonyme, nom, prenom);

		this.motDePasse = motDePasse;
		this.adresseMel = adresseMel;
		this.dateNaissance = dateNaissance;

		this.motDePasseParental = null;
		this.photo = UtilisateurHumain.photoDefaut;

		this.filtres = new HashSet<IFiltre>();
	}

	public UtilisateurHumain setMotDePasse(String motDePasse) {
		this.motDePasse = motDePasse;
		return this;
	}

	public UtilisateurHumain setAdresseMel(String adresseMel) {
		this.adresseMel = adresseMel;
		return this;
	}

	public UtilisateurHumain setDateNaissance(Date dateNaissance) {
		this.dateNaissance = dateNaissance;
		return this;
	}

	public UtilisateurHumain setMotDePasseParental(String motDePasseParental) {
		this.motDePasseParental = motDePasseParental;
		return this;
	}

	public UtilisateurHumain setPhoto(String photo) {
		this.photo = photo;
		return this;
	}

	public boolean verifieMotDePasse(String motDePasse) {
		return motDePasse.equals(this.motDePasse);
	}

	public boolean verifieMotDePasseParental(String motDePasseParental) {
		return motDePasseParental.equals(this.motDePasseParental);
	}

	public String getAdresseMel() { return this.adresseMel; }
	public String getPhoto() { return this.photo; }
	public Date getDateNaissance() { return this.dateNaissance; }

	public boolean ajouterFiltre(IFiltre filtre) {
		return this.filtres.add(filtre);
	}

	public boolean retirerFiltre(IFiltre filtre) {
		return this.filtres.remove(filtre);
	}
}
