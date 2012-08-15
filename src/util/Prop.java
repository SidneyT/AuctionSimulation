package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Prop {
    public static void main( String[] args )
    {
    	Properties prop = new Properties();
 
    	try {
    		//set the properties value
    		prop.setProperty("serverName", "localhost");
    		prop.setProperty("username", "root");
    		prop.setProperty("password", "triangle");
    		
    		prop.setProperty("timeUnitLength", "5");
    		prop.setProperty("", "");
    		prop.setProperty("extensionLength", "5");
 
    		//save properties to project root folder
    		prop.store(new FileOutputStream(propName), null);
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
    }
    
    private static Properties prop;
    private static String propName = "config.properties";
    public static Properties getProperties() {
    	try {
    		if (prop == null) {
    			prop = new Properties();
    			prop.load(new FileInputStream(propName));
    		}
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return prop;
    }
}
