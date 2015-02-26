import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Set;
import java.util.Iterator;

/////// Global Sku code //////
class code{
  public static int sku;
  public static int ordNum;
}

///////////////// Object for each item in hashmap ///////////
class InvItem {

  public int quantity;
  public int sku;
  public String name;
  
  public InvItem(String name, int quantity){
    this.quantity = quantity;
    this.sku = code.sku;
    this.name = name;
  }
}

///////////////// Object for record of orders in Queue ///////////
class orderItem{

  public int quantity;
  public int sku;
  public int orderNum;

  public orderItem(int quantity, int sku, int orderNum){
    this.quantity = quantity;
    this.sku = sku;
    this.orderNum = orderNum;
  }
}


/*
------------------------- Different Runnable Threads -------------------------
Insert thread, remove thread, available thread, purchase thread, cancel thread 
*/


///////////////// Insert new or modify quantity of already existing item. ///////////
class InsertItem implements Runnable {

  InvItem item;
  boolean exist;
  int sku;
  int number;
  ConcurrentMap cm;

  public InsertItem(ConcurrentMap cm, InvItem item, int number, boolean exist, int sku) {
    this.cm = cm;
    this.item = item;
    this.sku = sku;
    this.exist = exist;
    this.number = number;
  } 

  public void run() {
     
      InvItem a;

      if(!exist){
        cm.put(item.sku, item);
        System.out.println("A " + item.name + " has been inserted!");
      } else{
        a = (InvItem) cm.get(sku);
        a.quantity+=number;
      }


  }

}

////////////////////// Remove existing ////////////////
class RemoveItem implements Runnable {

    ConcurrentMap cm;
    int sku;

    public RemoveItem(ConcurrentMap cm, int sku) {
      this.cm = cm;
      this.sku = sku;
    }

   public void run() {
        InvItem tempItem = (InvItem) cm.remove(sku);
        System.out.println("The " + tempItem.name + " item has been removed from inventory.");
      }
  }

////////////////////// Check Availability ////////////////
class AItem implements Runnable {

    ConcurrentMap cm;
    int sku;

    public AItem(ConcurrentMap cm, int sku) {
      this.cm = cm;
      this.sku = sku;
    }

    public void run() {

      try{
        InvItem tempItem = (InvItem) cm.get(sku);

      if(tempItem.quantity != 0)
        System.out.println("Yes, there are " + tempItem.quantity +" "+ tempItem.name +" available.");
      else
        System.out.println("The item " + tempItem.name + " is out of stock.");

      } catch(NullPointerException ne){
        System.out.println("Item does not exist in Inventory.");
      }
    }

  }

////////////////////// Purchase Item ///////////////////
class PurchaseItem implements Runnable {

    ConcurrentMap cm;
    ConcurrentLinkedQueue clq;
    int sku;
    int number;

    public PurchaseItem(ConcurrentMap cm, ConcurrentLinkedQueue clq, int sku, int number) {
      this.sku = sku;
      this.cm = cm;
      this.clq = clq;
      this.number = number;
    }

    public void run() {

      orderItem tempOrder = new orderItem(number, sku,code.ordNum++);
      
      try{
        
        InvItem tempItem = (InvItem) cm.get(sku);

        synchronized(tempItem){ // Put a lock on currently purchased item

          if(tempItem.quantity > 0){
            tempItem.quantity-=number;
            clq.add(tempOrder);
            System.out.println("Purchase of " + tempItem.name + " confirmed and being delivered!"); 
          }
          else
            System.out.println("Sorry out of stock.");
        }
      } catch(NullPointerException ne){
        System.out.println("Item does not exist in inventory.");
      }
    
    }


  }

////////////////////// Return Item ///////////////////
class ReturnItem implements Runnable {

    ConcurrentMap cm;
    ConcurrentLinkedQueue clq;
    int sku;
    int orderNumber;

    public ReturnItem(ConcurrentMap cm, ConcurrentLinkedQueue clq, int orderNumber) {
      this.sku = sku;
      this.cm = cm;
      this.clq = clq;
      this.orderNumber = orderNumber;
    }

    public void run() {
      
      Iterator<orderItem> itr = clq.iterator();
      orderItem tempOrderItem = null;

      while(itr.hasNext()){

        tempOrderItem = itr.next();
        
        if(tempOrderItem.orderNum == orderNumber)
          break;

      }

      try{

        InvItem tempItem = (InvItem) cm.get(tempOrderItem.sku);
          tempItem.quantity+=tempOrderItem.quantity;
          clq.remove(tempOrderItem);
          System.out.println("Order of " + tempItem.name + " cancelled.");
         // could be removed before other operations
      } catch(NullPointerException ne){
        System.out.println("Item no longer exists!");
      }

    
  }
  }



//////////////// Main Class ////////////
public class Inventory {

  
  public  static void InsertItem(ConcurrentMap cm, String name, int number, boolean exist, int sku){
    
    Runnable a;

    if(!exist){
      InvItem tempItem = new InvItem(name, number);
      a = new InsertItem(cm, tempItem,number, false, code.sku++);
    } else{
      a = new InsertItem(cm, null, number, true,sku);
    }

    new Thread(a).start();
  } // Insert new or existing product


  public static void RemoveItem(ConcurrentMap cm, int sku){
      Runnable b = new RemoveItem(cm, sku);
      new Thread(b).start();
  } // Remove existing item

  public static void Available(ConcurrentMap cm, int sku){
    Runnable c = new AItem(cm, sku);
    new Thread(c).start();
  } // Check if item is avaialable

  public static void PurchaseItem(ConcurrentMap cm,  ConcurrentLinkedQueue clq, int sku, int number){
    Runnable d = new PurchaseItem(cm,clq, sku, number);
    new Thread(d).start();
  } // Purchase item 


  public static void ReturnItem(ConcurrentMap cm, ConcurrentLinkedQueue clq, int orderNumber){
    Runnable e = new ReturnItem(cm,clq,orderNumber);
    new Thread(e).start();
  } // Return item

  public static void main(String[] args) {

    ConcurrentMap<Integer,InvItem> cm = new ConcurrentHashMap<Integer,InvItem>();
    ConcurrentLinkedQueue<orderItem> clq = new ConcurrentLinkedQueue<orderItem>();

    System.out.println("Initializing inventory with items...");

    Inventory.InsertItem(cm ,"racket", 4, false, -1);
    Inventory.InsertItem(cm, "ball", 3, false, -1);
    Inventory.InsertItem(cm,"falcon",4,false,-1);
    Inventory.InsertItem(cm,"bear",1,false,-1);

   
    Inventory.InsertItem(cm,"",2,true,1);
    Inventory.InsertItem(cm,"",1,true,2);

    try{
      Thread.sleep(2000);
    } catch(InterruptedException e){
      e.printStackTrace();
    }
    System.out.println("Done.");
    System.out.println("");

     try{
      Thread.sleep(2000);
    } catch(InterruptedException e){
      e.printStackTrace();
    }

    // Note: 0 = racket, 1 = ball, 2 = falcon, 3 = bear

    Inventory.RemoveItem(cm,0); // Remove Racket from Inventory
    Inventory.Available(cm,1); // Is ball available?
    Inventory.Available(cm,0); // Is the racket available?
    Inventory.PurchaseItem(cm,clq,1,2); //Purchase 2 balls.
    Inventory.PurchaseItem(cm,clq,3,1); // Purchase one bear.
    Inventory.Available(cm,3); // Is bear available?
    Inventory.PurchaseItem(cm,clq,3,1); // Purchase one bear. (Should say out of stock.)
    Inventory.Available(cm,3); // IS bear available?
    Inventory.ReturnItem(cm, clq, 0); // Return balls.
    Inventory.ReturnItem(cm, clq, 1); // Return bear!
    Inventory.Available(cm,3); // Bear should be available.
    Inventory.InsertItem(cm,"",3,true,1); // Increase number of bears by 1
    Inventory.Available(cm,3); // Is bear available?
    Inventory.PurchaseItem(cm,clq,2,1); // Purchase 1 falcon.
    Inventory.Available(cm,3); // Is bear available?
    
    



}




}