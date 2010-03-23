/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.ItemValueFormatter;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.impl.options.TracerOptions;
import com.sun.tools.visualvm.modules.tracer.impl.details.DetailsPanel;
import com.sun.tools.visualvm.modules.tracer.impl.details.DetailsTableModel;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelineChart.Row;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 * All methods must be invoked from the EDT.
 *
 * @author Jiri Sedlacek
 */
public final class TimelineSupport {

    public static final int[] EMPTY_TIMESTAMPS = new int[0];

    private final TimelineChart chart;
    private final TimelineModel model;
    private final SynchronousXYItemsModel itemsModel;

    private final TimelineTooltipOverlay tooltips;
    private final TimelineUnitsOverlay units;

    private final List<TracerProbe> probes = new ArrayList();
    private final List<TimelineChart.Row> rows = new ArrayList();

    private int[] selectedTimestamps = new int[0];
    private final Set<SelectionListener> selectionListeners = new HashSet();


    // --- Constructor ---------------------------------------------------------

    public TimelineSupport() {
        // TODO: must be called in EDT!
        model = new TimelineModel();
        itemsModel = new SynchronousXYItemsModel(model);
        chart = new TimelineChart(itemsModel);
        tooltips = new TimelineTooltipOverlay(chart);
        chart.addOverlayComponent(tooltips);

        if (TracerOptions.getInstance().isShowValuesEnabled()) {
            units = new TimelineUnitsOverlay(chart);
            chart.addOverlayComponent(units);
        } else {
            units = null;
        }
    }


    // --- Chart access --------------------------------------------------------

    TimelineChart getChart() {
        return chart;
    }

    public ChartSelectionModel getChartSelectionModel() {
        return chart.getSelectionModel();
    }


    // --- Probes management ---------------------------------------------------

    public void addProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = chart.addRow();

                probes.add(probe);
                rows.add(row);

                ProbeItemDescriptor[] itemDescriptors = probe.getItemDescriptors();
                TimelineXYItem[] items = model.createItems(itemDescriptors);
                XYItemPainter[] painters  = new XYItemPainter[items.length];
                for (int i = 0; i < painters.length; i++)
                    painters[i] = TimelinePaintersFactory.createPainter(
                            itemDescriptors[i], i);
                
                row.addItems(items, painters);

                setupOverlays();
            }
        });
    }

    public void removeProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = getRow(probe);

                chart.removeRow(row);
                
                model.removeItems(row.getItems());

                rows.remove(row);
                probes.remove(probe);

                setupOverlays();
            }
        });
    }

    public List<TracerProbe> getProbes() {
        return probes;
    }

    public int getItemsCount() {
        return model.getItemsCount();
    }


    // --- Tooltips support ----------------------------------------------------

    private void setupOverlays() {
        final int rowsCount = chart.getRowsCount();

        TimelineTooltipPainter[] ttPainters = new TimelineTooltipPainter[rowsCount];
        
        for (int rowIndex = 0; rowIndex < ttPainters.length; rowIndex++) {
            final TimelineChart.Row row = chart.getRow(rowIndex);
            final TracerProbe probe = getProbe(row);

            final int itemsCount = row.getItemsCount();
            final String[] rowNames = new String[itemsCount];
            final ValueItemDescriptor[] viDescriptors = new ValueItemDescriptor[itemsCount];
            final String[] unitsStrings = new String[itemsCount];
            for (int itemIndex = 0; itemIndex < itemsCount; itemIndex++) {
                rowNames[itemIndex] = ((TimelineXYItem)row.getItem(itemIndex)).getName();
                viDescriptors[itemIndex] = (ValueItemDescriptor)probe.getItemDescriptors()[itemIndex];
                unitsStrings[itemIndex] = viDescriptors[itemIndex].getUnitsString(ItemValueFormatter.FORMAT_TOOLTIP);
            }

            ttPainters[rowIndex] = new TimelineTooltipPainter(new TimelineTooltipPainter.Model() {

                public int getRowsCount() {
                    return itemsCount;
                }

                public String getRowName(int index) {
                    return rowNames[index];
                }

                public String getRowValue(int index, long itemValue) {
                    return viDescriptors[index].getValueString(itemValue,
                            ItemValueFormatter.FORMAT_TOOLTIP);
                }

                public String getRowUnits(int index) {
                    return unitsStrings[index];
                }

            });
        }
        tooltips.setupPainters(ttPainters);

        if (units != null) units.setupModel(new TimelineUnitsOverlay.Model() {

            private final String LAST_UNITS_STRING = "lastUnitsString"; // NOI18N

            private Color[][] rowColors = new Color[rowsCount][];
            private String[][] rowMinValues = new String[rowsCount][];
            private String[][] rowMaxValues = new String[rowsCount][];

            private List<Color> visibleRowItemColors;
            private List<String> visibleRowItemMinValues;
            private List<String> visibleRowItemMaxValues;

            public void prefetch() {
                PaintersModel paintersModel = chart.getPaintersModel();
                for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
                    
                    Row row = chart.getRow(rowIndex);
                    TracerProbe probe = getProbe(row);
                    int rowItemsCount = row.getItemsCount();

                    ChartContext rowContext = row.getContext();
                    long commonMinY = rowContext.getDataOffsetY();
                    long commonMaxY = commonMinY + rowContext.getDataHeight();

                    if (visibleRowItemColors != null) {
                        visibleRowItemColors.clear();
                        visibleRowItemMinValues.clear();
                        visibleRowItemMaxValues.clear();
                    } else {
                        visibleRowItemColors = new ArrayList(rowItemsCount);
                        visibleRowItemMinValues = new ArrayList(rowItemsCount);
                        visibleRowItemMaxValues = new ArrayList(rowItemsCount);
                    }
                    
                    boolean sameFactorUnits = true;
                    double lastDataFactor = -1;
                    String lastUnitsString = LAST_UNITS_STRING;

                    for (int itemIndex = 0; itemIndex < rowItemsCount; itemIndex++) {
                        TimelineXYItem item = (TimelineXYItem)row.getItem(itemIndex);
                        TimelineXYPainter painter =
                                (TimelineXYPainter)paintersModel.getPainter(item);

                        if (painter.isPainting()) {
                            visibleRowItemColors.add(itemColor(painter));

                            ValueItemDescriptor descriptor = (ValueItemDescriptor)
                                    probe.getItemDescriptors()[itemIndex];

                            double dataFactor = descriptor.getDataFactor();
                            String unitsString = descriptor.getUnitsString(
                                    ItemValueFormatter.FORMAT_UNITS);
                            
                            if (sameFactorUnits) {
                                if (lastDataFactor == -1)
                                    lastDataFactor = dataFactor;
                                else if (lastDataFactor != dataFactor)
                                    sameFactorUnits = false;
                                lastDataFactor = dataFactor;
                                
                                if (lastUnitsString == LAST_UNITS_STRING)
                                    lastUnitsString = unitsString;
                                else if (!equals(lastUnitsString, unitsString))
                                    sameFactorUnits = false;
                                lastUnitsString = unitsString;
                            }

                            String minValueString = descriptor.getValueString(
                                    (long)(commonMinY / painter.dataFactor),
                                    ItemValueFormatter.FORMAT_UNITS);
                            visibleRowItemMinValues.add(unitsString == null ?
                                minValueString : minValueString + " " + unitsString);
                            
                            String maxValueString = descriptor.getValueString(
                                    (long)(commonMaxY / painter.dataFactor),
                                    ItemValueFormatter.FORMAT_UNITS);
                            visibleRowItemMaxValues.add(unitsString == null ?
                                maxValueString : maxValueString + " " + unitsString);
                        }
                    }

                    if (sameFactorUnits) {
                        rowColors[rowIndex] = new Color[] { null };
                        rowMinValues[rowIndex] =
                                new String[] { visibleRowItemMinValues.get(0) };
                        rowMaxValues[rowIndex] =
                                new String[] { visibleRowItemMaxValues.get(0) };
                    } else {
                        rowColors[rowIndex] = visibleRowItemColors.toArray(
                                new Color[visibleRowItemColors.size()]);
                        rowMinValues[rowIndex] = visibleRowItemMinValues.toArray(
                                new String[visibleRowItemMinValues.size()]);
                        rowMaxValues[rowIndex] = visibleRowItemMaxValues.toArray(
                                new String[visibleRowItemMaxValues.size()]);
                    }
                }
            }

            public Color[] getColors(Row row) {
                return rowColors[row.getIndex()];
            }

            public String[] getMinUnits(TimelineChart.Row row) {
                return rowMinValues[row.getIndex()];
            }

            public String[] getMaxUnits(TimelineChart.Row row) {
                return rowMaxValues[row.getIndex()];
            }

            private Color itemColor(TimelineXYPainter painter) {
                Color color = painter.lineColor;
                if (color == null) color = painter.fillColor;
                return color;
            }

            private boolean equals(String s1, String s2) {
                if (s1 == null) {
                    if (s2 == null) return true;
                    else return false;
                } else {
                    return s1.equals(s2);
                }
            }
            
        });
    }


    // --- Rows <-> Probes mapping ---------------------------------------------

    TimelineChart.Row getRow(TracerProbe probe) {
        return rows.get(probes.indexOf(probe));
    }

    TracerProbe getProbe(TimelineChart.Row row) {
        return probes.get(rows.indexOf(row));
    }


    // --- Values management ---------------------------------------------------

    public void addValues(final long timestamp, final long[] newValues) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int newRow = detailsModel == null ? -1 : detailsModel.getRowCount();
                model.addValues(timestamp, newValues);
                itemsModel.valuesAdded();
                if (newRow != -1) detailsModel.fireTableRowsInserted(newRow, newRow);
            }
        });
    }

    public void resetValues() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int lastRow = detailsModel == null ? -1 : detailsModel.getRowCount() - 1;
                model.reset();
                itemsModel.valuesReset();
                if (lastRow != -1) detailsModel.fireTableRowsDeleted(0, lastRow);
            }
        });
    }

    // --- Row selection management --------------------------------------------

    private AbstractTableModel detailsModel;

    void rowSelectionChanged() {
        notifySelectionChanged();
    }

    public TableModel getDetailsModel() {
        if (chart.isRowSelection()) detailsModel = createSelectionModel();
        else detailsModel = null;
        return detailsModel;
    }

    private AbstractTableModel createSelectionModel() {
        final List<SynchronousXYItem> selectedItems = getSelectedItems();
        final List<ValueItemDescriptor> selectedDescriptors = getSelectedDescriptors();
        int selectedItemsCount = selectedItems.size();
        
        final int columnCount = selectedItemsCount + 1;
        final SynchronousXYItem[] selectedItemsArr =
                selectedItems.toArray(new SynchronousXYItem[selectedItemsCount]);
        final String[] columnNames = new String[columnCount];
        columnNames[0] = "Time [ms]";
        for (int i = 1; i < columnCount; i++) {
            String itemName = selectedItemsArr[i - 1].getName();
            String unitsString = selectedDescriptors.get(i - 1).
                                 getUnitsString(ItemValueFormatter.FORMAT_DETAILS);
            String unitsExt = unitsString == null ? "" : " [" + unitsString + "]";
            columnNames[i] = itemName + unitsExt;
        }

        return new DetailsTableModel() {

            public int getRowCount() {
                return model.getTimestampsCount();
            }

            public int getColumnCount() {
                return columnCount;
            }

            public String getColumnName(int columnIndex) {
                return columnNames[columnIndex];
            }

            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 0) return DetailsPanel.class;
                return Long.class;
            }

            public ValueItemDescriptor getDescriptor(int columnIndex) {
                if (columnIndex == 0) return null;
                return selectedDescriptors.get(columnIndex - 1);
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) return model.getTimestamp(rowIndex);
                return selectedItemsArr[columnIndex - 1].getYValue(rowIndex);
            }

        };
    }


    // --- Time selection management -------------------------------------------

    public void setSelectedTimestamps(int[] selectedIndexes) {
        selectedTimestamps = selectedIndexes;
        List<SynchronousXYItem> selectedItems = getSelectedItems();
        List<ItemSelection> selections = new ArrayList(selectedItems.size());
        for (int selectedIndex : selectedIndexes)
            for (SynchronousXYItem selectedItem : selectedItems)
                selections.add(new XYItemSelection.Default(selectedItem,
                               selectedIndex, XYItemSelection.DISTANCE_UNKNOWN));
        chart.getSelectionModel().setSelectedItems(selections);
    }

    public int[] getSelectedTimestamps() {
        return selectedTimestamps;
    }


    private List<SynchronousXYItem> getSelectedItems() {
        List<TimelineChart.Row> selectedRows = chart.getSelectedRows();
        List<SynchronousXYItem> selectedItems = new ArrayList();
        for (TimelineChart.Row selectedRow : selectedRows)
            selectedItems.addAll(Arrays.asList(selectedRow.getItems()));
        return selectedItems;
    }

    private List<ValueItemDescriptor> getSelectedDescriptors() {
        List<TimelineChart.Row> selectedRows = chart.getSelectedRows();
        List selectedDescriptors = new ArrayList();
        for (TimelineChart.Row selectedRow : selectedRows)
            selectedDescriptors.addAll(Arrays.asList(getProbe(selectedRow).getItemDescriptors()));
        return selectedDescriptors;
    }


    // --- General selection support -------------------------------------------

    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }

    private void notifySelectionChanged() {
        for (SelectionListener selectionListener: selectionListeners)
            selectionListener.selectionChanged();
    }


    public static interface SelectionListener {

        void selectionChanged();

    }

}