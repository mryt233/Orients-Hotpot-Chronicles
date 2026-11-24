package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 盘/碗食材零穿插轻错层几何不变式测试：确保全尺寸×全装填数下无沉底、无同层共面与无上下穿插。
 * 作者：mryt233
 */
class PlateFoodLayoutTest {
    private static final PlateSize[] TEST_SIZES = {
        PlateSize.S1X1, PlateSize.S1X2, PlateSize.S1X3,
        PlateSize.S2X2, PlateSize.S1X5, PlateSize.S2X4,
        PlateSize.BOWL
    };

    @Test
    void allPlateSizesAndFillCountsAvoidFloorSinkOverlapAndCoplanarFaces() {
        for (PlateSize size : TEST_SIZES) {
            int cells = size.cells();
            double innerL = size.length() - 2 * PlateSize.RIM_T;
            double minBoundX = 8.0 - innerL / 2.0 - 1e-6;
            double maxBoundX = 8.0 + innerL / 2.0 + 1e-6;
            double outerHalfW = (size == PlateSize.S1X1 ? size.width() - 2 * PlateSize.RIM_T : size.width()) / 2.0;
            double minBoundZ = 8.0 - outerHalfW - 1e-6;
            double maxBoundZ = 8.0 + outerHalfW + 1e-6;

            for (int count = 1; count <= size.capacity(); count++) {
                List<PlateFoodLayout.SlotPose> poses = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int cell = i % cells;
                    int inCell = 0;
                    for (int j = cell; j < count; j += cells) {
                        inCell++;
                    }
                    PlateFoodLayout.SlotPose pose = PlateFoodLayout.pose(size, i, inCell);
                    poses.add(pose);

                    double halfFood = pose.foodPx() / 2.0;
                    double thickness = pose.foodPx() / 16.0;
                    double rad = Math.toRadians(pose.slopeDeg());
                    double cos = Math.cos(rad);
                    double sinAbs = Math.abs(Math.sin(rad));
                    double minY = pose.y() - (thickness / 2.0) * cos - halfFood * sinAbs;

                    assertTrue(minY >= PlateSize.FLOOR + 0.01,
                        size + " count=" + count + " idx=" + i + " minY=" + minY);
                    assertTrue(pose.cx() - halfFood * cos >= minBoundX && pose.cx() + halfFood * cos <= maxBoundX,
                        size + " count=" + count + " idx=" + i + " X out of inner bounds");
                    assertTrue(pose.cz() - halfFood >= minBoundZ && pose.cz() + halfFood <= maxBoundZ,
                        size + " count=" + count + " idx=" + i + " Z out of bounds");
                }

                for (int a = 0; a < poses.size(); a++) {
                    for (int b = a + 1; b < poses.size(); b++) {
                        PlateFoodLayout.SlotPose pa = poses.get(a);
                        PlateFoodLayout.SlotPose pb = poses.get(b);
                        double halfA = pa.foodPx() / 2.0;
                        double halfB = pb.foodPx() / 2.0;
                        if (Math.abs(pa.cx() - pb.cx()) >= halfA + halfB - 1e-6
                            || Math.abs(pa.cz() - pb.cz()) >= halfA + halfB - 1e-6) {
                            continue;
                        }
                        int layerA = size.layer(a);
                        int layerB = size.layer(b);
                        if (layerA == layerB) {
                            assertTrue(Math.abs(pa.y() - pb.y()) >= PlateFoodLayout.CELL_STAGGER - 1e-6,
                                size + " count=" + count + " coplanar tie a=" + a + " b=" + b);
                        } else {
                            PlateFoodLayout.SlotPose low = layerA < layerB ? pa : pb;
                            PlateFoodLayout.SlotPose high = layerA < layerB ? pb : pa;
                            double radL = Math.toRadians(low.slopeDeg());
                            double radH = Math.toRadians(high.slopeDeg());
                            double thickL = low.foodPx() / 16.0;
                            double thickH = high.foodPx() / 16.0;
                            double maxTop = low.y() + (thickL / 2.0) * Math.cos(radL) + (low.foodPx() / 2.0) * Math.abs(Math.sin(radL));
                            double minBot = high.y() - (thickH / 2.0) * Math.cos(radH) - (high.foodPx() / 2.0) * Math.abs(Math.sin(radH));
                            assertTrue(minBot - maxTop >= 0.02,
                                size + " count=" + count + " inter-layer overlap gap=" + (minBot - maxTop)
                                    + " a=" + a + " b=" + b);
                        }
                    }
                }
            }
        }
    }
}
