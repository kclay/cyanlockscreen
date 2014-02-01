package com.conceptualideas.cyanlockscreen;

import com.android.internal.widget.LockPatternView;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/18/14
 * Time: 5:56 PM
 */
public class LockPatternCell {

    static LockPatternView.Cell[][] sCells;


    static XCell[][] sXCells;


    public static class XCell {
        int row;
        int column;
        byte size;

        public XCell(int row, int column, byte size) {
            this.row = row;
            this.column = column;
            this.size = size;
        }
    }


    public static void init() {

/*
        findAndHookMethod(LockPatternView.Cell.class,"of",int.class,int.class,new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int row = (Integer)methodHookParam.args[0];
                int column = (Integer)methodHookParam.args[1];
                return of(row,column);
            }
        });*/
        // remove the range check that only allows 3x3 grids
        findAndHookMethod(LockPatternView.Cell.class, "checkRange", int.class, int.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                return null;
            }
        });

        updateSize(LockPatternUtilsHook.PATTERN_SIZE_DEFAULT);


    }

    public static synchronized LockPatternView.Cell of(int row, int column, byte size) {

        return sCells[row][column];
    }

    /**
     * @param row    The row of the cell.
     * @param column The column of the cell.
     */
    public static synchronized LockPatternView.Cell of(int row, int column) {
        return sCells[row][column];
    }


    public static void update() {
        setStaticObjectField(LockPatternView.Cell.class, "sCells", sCells);

    }

    private static final Class<?>[] params = new Class[]{int.class, int.class};

    public static void updateSize(byte size) {

        sCells = new LockPatternView.Cell[size][size];

        LockPatternView.Cell cell;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cell = (LockPatternView.Cell) newInstance(LockPatternView.Cell.class, params, i, j);
                //setIntField(cell,"row",i);
                //setIntField(cell,"column",j);
                sCells[i][j] = cell;

            }
        }
        update();

    }

    private static void checkRange(int row, int column, byte size) {
        if (row < 0 || row > size - 1) {
            throw new IllegalArgumentException("row must be in range 0-" + (size - 1));
        }
        if (column < 0 || column > size - 1) {
            throw new IllegalArgumentException("column must be in range 0-" + (size - 1));
        }
    }


}
