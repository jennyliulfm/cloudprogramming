package edu.utas.util;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.openstack.OSFactory;

import java.util.ArrayList;
import java.util.List;

public final class Utilities {

    public static ArrayList<Server> getWorkers(List<String[]> clientList) {
        //use to get worker from different client
        ArrayList<Server> serverList = new ArrayList<Server>();
        for (String[] info : clientList) {
            serverList.addAll(getServersByClient(OSFactory.builderV3()
                    .endpoint(PropertiesCache.getInstance().getProperty("OS_ENDPOINT"))
                    .credentials(info[0], info[1], Identifier.byName("Default"))
                    .scopeToProject(Identifier.byId(info[2]))
                    .authenticate()));
        }

        return serverList;
    }

    public static ArrayList<Server> getServersByClient(OSClient.OSClientV3 os)
    {
        return new ArrayList<>( os.compute().servers().list());
    }

    public static String getServerIPAddress(Server s)
    {
        String[] addr = s.getAddresses().getAddresses().toString().split("[=,]");
        return addr[2];
    }

    public static String getServerIPByID(String id, List<String[]> clientList)
    {
        ArrayList<Server> workers = getWorkers(clientList);
        for(Server s: workers)
        {
            if(s.getId().equals(id))
            {
                return getServerIPAddress(s);
            }
        }
        return null;
    }

    public static ArrayList<Image> getImages(List<String[]> clientList)
    {
        ArrayList<Image> imageList = new ArrayList<Image>();
        for (String[] info : clientList) {
            imageList.addAll(getImagesByClient(OSFactory.builderV3()
                    .endpoint(PropertiesCache.getInstance().getProperty("OS_ENDPOINT"))
                    .credentials(info[0], info[1], Identifier.byName("Default"))
                    .scopeToProject(Identifier.byId(info[2]))
                    .authenticate()));
        }
        return imageList;
    }

    public static Image getImageByName(List<String[]> clientList, String name)
    {
        ArrayList<Image> images = getImages(clientList);
        for(Image img : images)
        {
            if (img.getName().equals(name))
            {
                return img;
            }
        }

        return null;
    }

    public static ArrayList<Image> getImagesByClient(OSClient.OSClientV3 os)
    {
        ArrayList<Image> allImages = new ArrayList<Image>(os.compute().images().list());
        ArrayList<Image> snapshots = new ArrayList<Image>();
        for(Image img : allImages)
        {
            if (img.isSnapshot())
            {
                snapshots.add(img);
            }
        }
        return  snapshots;
    }
}
