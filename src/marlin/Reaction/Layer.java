package marlin.Reaction;

import marlin.I;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Layer extends ArrayList<I.Show> implements I.Show{
    public String name;
    public static HashMap<String,Layer> byName = new HashMap<>();
    public static Layer ALL = new Layer("ALL");
    public Layer(String name){
        this.name = name;
        if(!name.equals("All")){
            ALL.add(this);
        }
        byName.put(name, this);
    }
    
    @Override
    public void show(Graphics g){
        for (I.Show item : this){
            item.show(g);
        }
    }
}
