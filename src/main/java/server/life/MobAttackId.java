/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life;

/**
 *
 * @author Tosho
 */
class MobAttackId {

    public int id;
    public int attack;

    public MobAttackId(int id, int attack) {
        this.id = id;
        this.attack = attack;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        MobAttackId other = (MobAttackId) obj;
        return this.id == other.id && this.attack == other.attack;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.id;
        hash = 71 * hash + this.attack;
        return hash;
    }

}
