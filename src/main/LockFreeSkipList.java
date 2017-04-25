import java.lang.reflect.Array;
import java.util.concurrent.atomic.*;

public class LockFreeSkipList<T extends Comparable<T>> implements SkipList<T> {
	   
    private static final int MaxHeight = 32;
    
   Node head; 

    private class Node {
        T value;
        NodeArray nexts;
        AtomicBoolean marked;
        boolean fullyLinked;
        int topLevel;

        Node(T val, int topLevel) {
            value = val;
            nexts = new NodeArray(MaxHeight);
            marked = new AtomicBoolean(false);
            fullyLinked = false;
            this.topLevel = topLevel;
        }  
    }

    private class NodeArray {
        private Node[] arr;

        NodeArray(int n) {
            arr = (Node[]) Array.newInstance(Node.class, n);
        }

        Node get(int i) {
            return arr[i];
        }

        void set(int i, Node node) {
            arr[i] = node;
        }
        
    }

    /**
     * Public functions
     */
    
    public LockFreeSkipList() {
        head = new Node(null, MaxHeight);
        Node tail = new Node(null, MaxHeight);
        for (int i = 0; i < MaxHeight; i++) {
            head.nexts.set(i, tail);
        }
    }

    public boolean add(T val) {
        int topLayer = randomLevel(MaxHeight);
        NodeArray preds = new NodeArray(MaxHeight);
        NodeArray succs = new NodeArray(MaxHeight);
        while(true){
            int lFound = findNode(val, preds, succs);
            if(lFound!=-1){
                Node nodeFound = succs.get(lFound);
                if(!nodeFound.marked.get()){
                    while(!nodeFound.fullyLinked);
                    return false;
                }
                continue;
            }
            
            Node pred, succ, prevPred = null;
            boolean valid = true;
            Node newNode = new Node(val, topLayer);
            AtomicMarkableReference<Node> atomicNode = new AtomicMarkableReference<Node>(preds.get(0).nexts.get(0), preds.get(0).marked.get());
            for(int layer=0; layer<=topLayer; layer++){
                pred = preds.get(layer);
                succ = succs.get(layer);
                
                atomicNode.set(pred.nexts.get(layer), pred.marked.get());
                
                newNode.nexts.set(layer, succ);
                if (!atomicNode.compareAndSet(succ, newNode, false, true)){
                    valid = false;
                    break;
                }
                preds.get(layer).nexts.set(layer, atomicNode.getReference());
            }
            
            newNode.fullyLinked = true;
            break;
        }
        return true;
    }
    
    public boolean remove(T val) {
        Node nodeToDelete = null;
        boolean isMarked = false;
        int topLevel = -1;
        NodeArray preds = new NodeArray(MaxHeight);
        NodeArray succs = new NodeArray(MaxHeight);
        while(true) {
            int found = findNode(val, preds, succs);
            if (isMarked || (found != -1 && okToDelete(succs.get(found), found))) {
                if(!isMarked) {
                    nodeToDelete = succs.get(found);
                    topLevel = nodeToDelete.topLevel;
                    if(!nodeToDelete.marked.compareAndSet(false, true)) {
                    	return false;
                    }
                    isMarked = true;
                }

                Node succ, pred = null;
                boolean valid = true;
                AtomicMarkableReference<Node> atomicNode = new AtomicMarkableReference<Node>(preds.get(0).nexts.get(0), preds.get(0).marked.get());
                for (int i = 0; i <= topLevel; i++) {
                    pred = preds.get(i);
                    succ = succs.get(i);
                    
                    atomicNode.set(pred.nexts.get(i), pred.marked.get());

                    if (!atomicNode.compareAndSet(succ, nodeToDelete.nexts.get(i), false, true)){
                        valid = false;
                        break;
                    }
                    preds.get(i).nexts.set(i, atomicNode.getReference());
                    
                }
                if (!valid) continue;
                return true;
            } else {
                return false;
            }
        }
    }
    
    public boolean contains(T val) {
        NodeArray preds = new NodeArray(MaxHeight);
        NodeArray succs = new NodeArray(MaxHeight);
        int lFound = findNode(val, preds, succs);
        return  (lFound!=-1) 
                && (succs.get(lFound).fullyLinked) 
                && (!succs.get(lFound).marked.get());
    }
    
    /**
     * prints the list
     */
    public void print(){
        
    }

    /**
     * Helper (internal) functions
     */
    
    /**
     * @return the highest layer at which value exists or -1 if value doesn't exist
     */
    private int findNode(T val, NodeArray preds, NodeArray succs) {
        int lFound = - 1;
        Node pred = head;
        for (int level = MaxHeight - 1; level >= 0; level--) {
            Node curr = pred.nexts.get(level);
            
            while (curr.value != null && greaterThan(val, curr.value)) {
                pred = curr; curr = pred.nexts.get(level);
            }

            if (lFound == -1 && equalTo(val, curr.value)) {
                lFound = level;
            }
            preds.set(level, pred);
            succs.set(level, curr);
        }
        return lFound;
    }


    /**
     *@return whether the node at the level it was found can be deleted
     */
    private boolean okToDelete(Node node, int level) {
        return node.fullyLinked && !node.marked.get() && node.topLevel == level;
    }

    
    private int randomLevel(int v){
        int lvl = (int)(Math.log(1.-Math.random())/Math.log(1.-0.5));
        return Math.min(lvl, v);
    }
    
    /******************************************************************************
     * Utility Functions                                                           *
     ******************************************************************************/

     private boolean lessThan(T a, T b) {
         return a.compareTo(b) < 0;
     }

     private boolean equalTo(T a, T b) {
         if (a == null || b == null) { return false; }
         return a.compareTo(b) == 0;
     }

     private boolean greaterThan(T a, T b) {
         if (a == null) { return false; }
         if (b == null) { return true; }
         return a.compareTo(b) > 0;
     }
}
