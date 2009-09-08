package net.javacoding.jspider.api.event.folder;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Folder;


/**
 *
 * $Id: FolderDiscoveredEvent.java,v 1.2 2003/03/28 17:26:25 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class FolderDiscoveredEvent extends FolderRelatedEvent {

    public FolderDiscoveredEvent(Folder folder) {
        super(folder);
    }

    public String getComment() {
        return "folder " + folder + " discovered";
    }

    public Folder getFolder ( ) {
        return super.getFolder ( );
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
