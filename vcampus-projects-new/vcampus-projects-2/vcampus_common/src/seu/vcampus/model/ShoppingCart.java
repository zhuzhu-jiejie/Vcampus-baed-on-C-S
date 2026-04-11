package seu.vcampus.model;

import java.util.ArrayList;
import java.util.List;

import seu.vcampus.model.CartItem;

public class ShoppingCart {
    private static ShoppingCart instance;
    private List<CartItem> items;
    
    private ShoppingCart() {
        items = new ArrayList<>();
    }
    
    public static ShoppingCart getInstance() {
        if (instance == null) {
            instance = new ShoppingCart();
        }
        return instance;
    }
    
    public void addItem(CartItem item) {
        // 检查是否已存在相同商品
        for (CartItem existingItem : items) {
            if (existingItem.getProduct().getId().equals(item.getProduct().getId())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
    }
    
    public void removeItem(CartItem item) {
        items.remove(item);
    }
    
    public void updateItem(CartItem item) {
        // 更新已在列表中，无需额外操作
    }
    
    public List<CartItem> getItems() {
        return new ArrayList<>(items); // 返回副本以避免外部修改
    }
    
    public void clear() {
        items.clear();
    }
    
    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }
}