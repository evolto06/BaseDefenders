package systems;

import entities.Entity;
import entities.EntityManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import main.World;


public class Renderer extends JPanel implements GameSystem {
    World world;
    Graphics g = this.getGraphics();
    EntityManager em;
    public Renderer(World world){
        this.world = world;
        
    }


    @Override
    public void update() {
        paintComponent(g);
    }
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        draw(g2);
        g2.dispose();
        
    }

    public void draw(Graphics2D g2){
        world.em.getAllEntities().forEach((Integer id, Entity entity) -> {
            if(entity.hasComponent(components.Position.class) && entity.hasComponent(components.Sprite.class)){
                components.Position pos = em.getComponent(id, components.Position.class);
                components.Sprite sprite = em.getComponent(id, components.Sprite.class);
                if(sprite.isLoaded()){
                    g2.drawImage(sprite.getSprite(), pos.x, pos.y, null);
                }
               
            }
        });
    }
}