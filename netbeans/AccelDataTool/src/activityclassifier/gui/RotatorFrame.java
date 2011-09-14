/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * RotatorFrame.java
 *
 * Created on 10/06/2011, 10:54:10 AM
 */

package activityclassifier.gui;

import activityclassifier.utils.CalcStatistics;
import activityclassifier.utils.RotateSamplesToVerticalHorizontal;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;

/**
 *
 * @author abd01c
 */
public class RotatorFrame extends javax.swing.JFrame {

	private static final String PASTE_MIME = "text/plain";
	private static final Class PASTE_CLASS = String.class;
	private static final DataFlavor PASTE_FLAVOUR;

	static {
		DataFlavor flav = null;
		try {
			flav = new DataFlavor(PASTE_MIME + ";class=" + PASTE_CLASS.getCanonicalName()+";charset=unicode");
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(RotatorFrame.class.getName()).log(Level.SEVERE, null, ex);
		}
		PASTE_FLAVOUR = flav;
	}

	private CalcStatistics origStatistics = new CalcStatistics(3);
	private RotateSamplesToVerticalHorizontal rstvh = new RotateSamplesToVerticalHorizontal();
	private float[][] lastRotatedData;

    /** Creates new form RotatorFrame */
    public RotatorFrame() {
        initComponents();

		handlePasteEvent(origAccelX);
		handlePasteEvent(origAccelY);
		handlePasteEvent(origAccelZ);
	}

	private void handlePasteEvent(final JTextArea jTextArea) {
		final Action defAction = jTextArea.getActionMap().get(DefaultEditorKit.pasteAction);
		jTextArea.getActionMap().put(DefaultEditorKit.pasteAction,
				new AbstractAction() {
						public void actionPerformed(ActionEvent e) {
							processKeyboardPasteAction(e, jTextArea, defAction);
						}
				}
			   );
	}

	private void processKeyboardPasteAction(ActionEvent e, JTextArea origArea, Action defAction) {
		Clipboard clipboard = this.getToolkit().getSystemClipboard();

		boolean isCorrectFormat = false;

		if (clipboard.isDataFlavorAvailable(PASTE_FLAVOUR)) {
			try {
				Reader reader = PASTE_FLAVOUR.getReaderForText(clipboard.getContents(null));
				Scanner scanner = new Scanner(reader);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.contains("\t")) {
						isCorrectFormat = true;
						break;
					}
				}
				reader.close();
			} catch (UnsupportedFlavorException ex) {
				Logger.getLogger(RotatorFrame.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(RotatorFrame.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		if (isCorrectFormat) {
			pasteData();
		} else {
			defAction.actionPerformed(e);
		}
	}

	private void pasteData()
	{
		Clipboard clipboard = this.getToolkit().getSystemClipboard();
		
		List<StringBuilder> columns = new ArrayList<StringBuilder>();
		columns.add(new StringBuilder());

		try {
			Reader reader = PASTE_FLAVOUR.getReaderForText(clipboard.getContents(null));
			Scanner scanner = new Scanner(reader);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.contains("\t")) {
					String values[] = line.split("\t");
					for (int i=0; i<values.length; ++i) {
						if (i>columns.size()-1)
							columns.add(new StringBuilder());

						columns.get(i).append(values[i]).append("\n");
					}
				} else {
					columns.get(0).append(line).append("\n");
				}
			}
			reader.close();
		} catch (UnsupportedFlavorException ex) {
			Logger.getLogger(RotatorFrame.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(RotatorFrame.class.getName()).log(Level.SEVERE, null, ex);
		}

		origAccelX.setText(columns.get(0).toString());

		if (columns.size()>1)
			origAccelY.setText(columns.get(1).toString());
		else
			origAccelY.setText("");

		if (columns.size()>2)
			origAccelZ.setText(columns.get(2).toString());
		else
			origAccelZ.setText("");

		if (columns.size()>3)
			JOptionPane.showMessageDialog(this, "You pasted more than 3 columns!\nOnly 3 are supported.");
	}

	private void copyData()
	{
		if (lastRotatedData==null) {
			JOptionPane.showMessageDialog(this, "You need to convert some data first!");
			return;
		}

		Clipboard clipboard = this.getToolkit().getSystemClipboard();
		StringBuilder builder = new StringBuilder();

		for (int i=0; i<lastRotatedData.length; ++i) {
			if (i!=0)
				builder.append("\n");

			for (int d=0; d<lastRotatedData[i].length; ++d) {
				if (d!=0)
					builder.append("\t");
				
				builder.append(Float.toString(lastRotatedData[i][d]));
			}
		}

		StringSelection data = new StringSelection(builder.toString());

		clipboard.setContents(data, data);
		
	}

	
//	private int computeNumOfLines(javax.swing.JTextArea textArea) {
//		String text = textArea.getText();
//
//		int count = 0;
//		int start = 0;
//		int len = text.length();
//
//		while (start>0 && start<len) {
//			start = text.indexOf(13, start);
//			if (start>0) {
//				++start;
//				++count;
//			}
//		}
//
//		return count;
//	}

	private List<Float> convertToFloats(String numberList)
	{
		ArrayList<Float> result = new ArrayList<Float>();

		Scanner scanner = new Scanner(numberList);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();

			String number = line.replaceAll("[^0-9.e-]", "");

			Float f = Float.parseFloat(number);

			result.add(f);
		}

		return result;
	}

	private String[] convertToStrings(float[][] data)
	{
		int dim = data[0].length;

		StringBuilder builders[] = new StringBuilder[dim];

		for (int d=0; d<dim; ++d) {
			builders[d] = new StringBuilder();
		}

		for (int i=0; i<data.length; ++i) {
			for (int d=0; d<dim; ++d) {
				builders[d].append(Float.toString(data[i][d])).append("\n");
			}
		}

		String[] result = new String[dim];

		for (int d=0; d<dim; ++d) {
			result[d] = builders[d].toString();
		}

		return result;
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        origAccelX = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        origAccelY = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel5 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane3 = new javax.swing.JScrollPane();
        origAccelZ = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel7 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane4 = new javax.swing.JScrollPane();
        finalAccelX = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel9 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane5 = new javax.swing.JScrollPane();
        finalAccelY = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel10 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane6 = new javax.swing.JScrollPane();
        finalAccelZ = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel6 = new javax.swing.JPanel();
        btnPaste = new javax.swing.JButton();
        btnConvert = new javax.swing.JButton();
        btnCopy = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        lblGravityX = new javax.swing.JLabel();
        lblGravityY = new javax.swing.JLabel();
        lblGravityZ = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Data Rotator");
        setAlwaysOnTop(true);
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        jPanel3.setLayout(new java.awt.GridLayout(0, 3, 5, 5));

        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Accel X Data");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setOpaque(true);
        jPanel2.add(jLabel1, java.awt.BorderLayout.NORTH);

        origAccelX.setColumns(20);
        origAccelX.setRows(5);
        jScrollPane1.setViewportView(origAccelX);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel2);

        jPanel4.setBackground(new java.awt.Color(0, 255, 0));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Accel Y Data");
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel2.setOpaque(true);
        jPanel4.add(jLabel2, java.awt.BorderLayout.NORTH);

        origAccelY.setColumns(20);
        origAccelY.setRows(5);
        jScrollPane2.setViewportView(origAccelY);

        jPanel4.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel4);

        jPanel5.setBackground(new java.awt.Color(0, 0, 255));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Accel Z Data");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel3.setOpaque(true);
        jPanel5.add(jLabel3, java.awt.BorderLayout.NORTH);

        origAccelZ.setColumns(20);
        origAccelZ.setRows(5);
        jScrollPane3.setViewportView(origAccelZ);

        jPanel5.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel5);

        jPanel7.setBackground(new java.awt.Color(255, 0, 0));
        jPanel7.setLayout(new java.awt.BorderLayout());

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Hor 1 Data");
        jLabel4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel4.setOpaque(true);
        jPanel7.add(jLabel4, java.awt.BorderLayout.NORTH);

        finalAccelX.setColumns(20);
        finalAccelX.setEditable(false);
        finalAccelX.setRows(5);
        jScrollPane4.setViewportView(finalAccelX);

        jPanel7.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel7);

        jPanel9.setBackground(new java.awt.Color(0, 255, 0));
        jPanel9.setLayout(new java.awt.BorderLayout());

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Hor 2 Data");
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel5.setOpaque(true);
        jPanel9.add(jLabel5, java.awt.BorderLayout.NORTH);

        finalAccelY.setColumns(20);
        finalAccelY.setEditable(false);
        finalAccelY.setRows(5);
        jScrollPane5.setViewportView(finalAccelY);

        jPanel9.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel9);

        jPanel10.setBackground(new java.awt.Color(0, 0, 255));
        jPanel10.setLayout(new java.awt.BorderLayout());

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Ver Data");
        jLabel6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel6.setOpaque(true);
        jPanel10.add(jLabel6, java.awt.BorderLayout.NORTH);

        finalAccelZ.setColumns(20);
        finalAccelZ.setEditable(false);
        finalAccelZ.setRows(5);
        jScrollPane6.setViewportView(finalAccelZ);

        jPanel10.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel10);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel6.setMaximumSize(new java.awt.Dimension(100, 32767));
        jPanel6.setMinimumSize(new java.awt.Dimension(100, 0));
        jPanel6.setPreferredSize(new java.awt.Dimension(100, 100));

        btnPaste.setText("Paste");
        btnPaste.setMinimumSize(new java.awt.Dimension(80, 25));
        btnPaste.setPreferredSize(new java.awt.Dimension(80, 25));
        btnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasteActionPerformed(evt);
            }
        });
        jPanel6.add(btnPaste);

        btnConvert.setText("Convert");
        btnConvert.setMinimumSize(new java.awt.Dimension(80, 25));
        btnConvert.setPreferredSize(new java.awt.Dimension(80, 25));
        btnConvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConvertActionPerformed(evt);
            }
        });
        jPanel6.add(btnConvert);

        btnCopy.setText("Copy");
        btnCopy.setMinimumSize(new java.awt.Dimension(80, 25));
        btnCopy.setPreferredSize(new java.awt.Dimension(80, 25));
        btnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyActionPerformed(evt);
            }
        });
        jPanel6.add(btnCopy);

        btnClear.setText("Clear All");
        btnClear.setMinimumSize(new java.awt.Dimension(80, 25));
        btnClear.setPreferredSize(new java.awt.Dimension(80, 25));
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        jPanel6.add(btnClear);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel1.setMinimumSize(new java.awt.Dimension(152, 25));

        lblGravityX.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblGravityX.setText("Gravity X");
        lblGravityX.setMaximumSize(new java.awt.Dimension(100, 25));
        lblGravityX.setMinimumSize(new java.awt.Dimension(100, 25));
        lblGravityX.setOpaque(true);
        lblGravityX.setPreferredSize(new java.awt.Dimension(100, 25));
        jPanel1.add(lblGravityX);

        lblGravityY.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblGravityY.setText("Gravity Y");
        lblGravityY.setMaximumSize(new java.awt.Dimension(100, 25));
        lblGravityY.setMinimumSize(new java.awt.Dimension(100, 25));
        lblGravityY.setOpaque(true);
        lblGravityY.setPreferredSize(new java.awt.Dimension(100, 25));
        jPanel1.add(lblGravityY);

        lblGravityZ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblGravityZ.setText("Gravity Z");
        lblGravityZ.setMaximumSize(new java.awt.Dimension(100, 25));
        lblGravityZ.setMinimumSize(new java.awt.Dimension(100, 25));
        lblGravityZ.setOpaque(true);
        lblGravityZ.setPreferredSize(new java.awt.Dimension(100, 25));
        jPanel1.add(lblGravityZ);

        jPanel6.add(jPanel1);

        getContentPane().add(jPanel6, java.awt.BorderLayout.SOUTH);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-608)/2, (screenSize.height-435)/2, 608, 435);
    }// </editor-fold>//GEN-END:initComponents

	private void btnConvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConvertActionPerformed

		List<Float> xList = convertToFloats(origAccelX.getText());
		List<Float> yList = convertToFloats(origAccelY.getText());
		List<Float> zList = convertToFloats(origAccelZ.getText());

		if (xList.size()!=yList.size() || xList.size()!=zList.size() || yList.size()!=zList.size()) {
			JOptionPane.showMessageDialog(this,
					"Make sure you have equal numbers of x, y and z numbers\n" +
					"[x="+xList.size()+", y="+yList.size()+", z="+zList.size()+"]"
					);
			return;
		}

		int size = xList.size();

		float[][] origData = new float[size][3];

		for (int i=0; i<size; ++i) {
			origData[i][0] = xList.get(i);
			origData[i][1] = yList.get(i);
			origData[i][2] = zList.get(i);
		}

		rstvh.rotateToWorldCoordinates(origData);

		String[] results = convertToStrings(origData);

		finalAccelX.setText(results[0]);
		finalAccelY.setText(results[1]);
		finalAccelZ.setText(results[2]);

		lblGravityX.setText(Float.toString(rstvh.getGravityVec()[0]));
		lblGravityY.setText(Float.toString(rstvh.getGravityVec()[1]));
		lblGravityZ.setText(Float.toString(rstvh.getGravityVec()[2]));

		lastRotatedData = origData;


	}//GEN-LAST:event_btnConvertActionPerformed

	private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed


	}//GEN-LAST:event_formKeyPressed

	private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed

		origAccelX.setText("");
		origAccelY.setText("");
		origAccelZ.setText("");
		
	}//GEN-LAST:event_btnClearActionPerformed

	private void btnPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPasteActionPerformed

		Clipboard clipboard = this.getToolkit().getSystemClipboard();

		if (clipboard.isDataFlavorAvailable(PASTE_FLAVOUR)) {
			pasteData();
		}

	}//GEN-LAST:event_btnPasteActionPerformed

	private void btnCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopyActionPerformed

		copyData();

	}//GEN-LAST:event_btnCopyActionPerformed




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnConvert;
    private javax.swing.JButton btnCopy;
    private javax.swing.JButton btnPaste;
    private javax.swing.JTextArea finalAccelX;
    private javax.swing.JTextArea finalAccelY;
    private javax.swing.JTextArea finalAccelZ;
    private javax.swing.JLabel lblGravityX;
    private javax.swing.JLabel lblGravityY;
    private javax.swing.JLabel lblGravityZ;
    private javax.swing.JTextArea origAccelX;
    private javax.swing.JTextArea origAccelY;
    private javax.swing.JTextArea origAccelZ;
    // End of variables declaration//GEN-END:variables


}
