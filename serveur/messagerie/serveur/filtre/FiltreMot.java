package messagerie.serveur.filtre;

public class FiltreMot implements IFiltre {
	private String mot;
	
	public FiltreMot(String mot) {
		this.mot = mot;
	}

	@Override
	public boolean compare(Object objet) {
		if (objet instanceof String)
			return ((String)objet).equals(this.mot);
		return false;
	}

	@Override
	public boolean equals(Object objet) {
		if (objet == this)
			return true;

		if (objet.getClass() != this.getClass())
			return false;

		return ((FiltreMot)objet).mot.equals(this.mot);
	}

	@Override
	public int hashCode() {
		return (this.mot.hashCode() % 512) * 
				this.mot.hashCode();
	}
}
