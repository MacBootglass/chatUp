package messagerie.serveur.discussion;

import java.util.ArrayList;
import java.util.List;

import messagerie.serveur.utilisateur.Utilisateur;
import messagerie.serveur.exception.*;

public class DiscussionTexte extends Discussion {
    public final static long serialVersionUID = 7521L;

    private List<Message> messages;
    
    public DiscussionTexte(List<Utilisateur> utilisateurs) throws DiscussionException {
        super(utilisateurs);
        this.messages = new ArrayList<Message>();
    }
    public boolean addMessage(Message message){
        return messages.add(message);
    }
    public List<Message> getMessages(){
        return this.messages;
    }
}