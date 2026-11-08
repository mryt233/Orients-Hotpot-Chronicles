/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
public class ClientViewGroup<T> {
    public static <IN, OUT> List<ClientViewGroup<OUT>> map(List<ViewGroup<IN>> groups,
            Function<IN, OUT> mapper, BiConsumer<ViewGroup<IN>, ClientViewGroup<OUT>> consumer) {
        throw new UnsupportedOperationException();
    }
}
