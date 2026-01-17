package threeadd.glimpsy.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

public class SerializationUtil {

    public static String itemStackToJson(ItemStack item) {
        Gson gson = new Gson();
        Map<String, Object> map = item.serialize();
        return gson.toJson(map);
    }

    public static ItemStack itemStackFromJson(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = gson.fromJson(json, type);
        return ItemStack.deserialize(map);
    }
}
