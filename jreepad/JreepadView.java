package jreepad;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;

/*

The original free Windows version is 380Kb

Todo:
- Drag-and-drop of nodes
- Menus and the actions they entail
- Toolbar actions
- Search for text
- The article needs to resize properly, EVERY time its container (its scrollpane) is resized

*/

public class JreepadView extends Box
{
  private JreepadNode root;
  private JreepadNode currentNode;
  private JreepadNode currentDragDropNode;
  private TreeNode topNode;
  private JreepadTreeModel treeModel;
  private JTree tree;
  private JScrollPane treeView;
  private JScrollPane articleView;
  private JEditorPane editorPane;
  private JSplitPane splitPane;

  public JreepadView()
  {
    this(new JreepadNode());
  }
  
  public JreepadView(JreepadNode root)
  {
    super(BoxLayout.X_AXIS);
    treeView = new JScrollPane();
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setResizeWeight(0.5);

    this.root = root;

/* DEPRECATED - HOPEFULLY!
    topNode = new DefaultMutableTreeNode(root);

    // Now set up all the JTree, DefaultTreeNode, stuff
    createNodes(topNode, root);

    treeModel = new DefaultTreeModel(topNode);
*/
    treeModel = new JreepadTreeModel(root);
    treeModel.addTreeModelListener(new JreepadTreeModelListener());

    tree = new JTree(treeModel);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setExpandsSelectedPaths(true);
    tree.setInvokesStopCellEditing(true);
    tree.setEditable(true);
    
    tree.setModel(treeModel);

    DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);
    renderer.setLeafIcon(null);
    tree.setCellRenderer(renderer);

    //Listen for when the selection changes.
    tree.addTreeSelectionListener(new TreeSelectionListener()
                   {
                     public void valueChanged(TreeSelectionEvent e)
                     {
                        JreepadNode node = (JreepadNode)
                           tree.getLastSelectedPathComponent();
                        if (node == null) return;

                      //  JreepadNode nodeInfo = (JreepadNode)(node.getUserObject());
                        setCurrentNode(node);
                      }
                   }); 

    // Add mouse listener - this will be used to implement drag-and-drop, context menu (?), etc
    MouseListener ml = new MouseAdapter()
    {
      public void mousePressed(MouseEvent e)
      {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
//        System.out.println("Mouse pressed: path = " + selPath);
        if(selPath != null)
        {
          currentDragDropNode = (JreepadNode)selPath.getLastPathComponent();
          // if(e.getClickCount() == 1) {mySingleClick(selPath);}
//            System.out.println("Setting dragdrop node to " + currentDragDropNode);
        }
      }
      public void mouseReleased(MouseEvent e)
      {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
//        System.out.println("Mouse released: path = " + selPath);
        if(selPath != null)
        {
          if(currentDragDropNode != null && 
             currentDragDropNode.getParentNode() != null && 
             currentDragDropNode.getParentNode() != (JreepadNode)selPath.getLastPathComponent() && 
             currentDragDropNode != (JreepadNode)selPath.getLastPathComponent())
          {
            // Then we need to perform a drag-and-drop operation!
//            System.out.println("Drag-and-drop event occurred!");
            moveNode(currentDragDropNode, (JreepadNode)selPath.getLastPathComponent());
          }
        }
        currentDragDropNode = null;
      }
      public void mouseClicked(MouseEvent e)
      {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
 //       System.out.println("Mouse clicked: path = " + selPath);
        if(selPath != null)
        {
          if(e.isPopupTrigger())
          {
            // Now we can implement the pop-up content menu
            System.out.println("Context menu would be launched here!");
          }
        }
      }
    };
    tree.addMouseListener(ml); 
 
 
    treeView.setViewportView(tree);


    editorPane = new JEditorPane("text/plain", root.getContent());
    editorPane.setEditable(true);
    articleView = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    articleView.addComponentListener(new ComponentListener()
    					{
    					  public void componentResized(ComponentEvent e)
    					  {
    					    editorPane.setSize(articleView.getViewport().getViewSize());
    					  }
    					  public void componentMoved(ComponentEvent e){}
    					  public void componentHidden(ComponentEvent e){}
    					  public void componentShown(ComponentEvent e){}
    					}
    					);

    setViewBoth();
    setCurrentNode(root);
    tree.setSelectionRow(0);
  }

  public void setViewBoth()
  {   
      splitPane.setLeftComponent(treeView);    splitPane.setRightComponent(articleView);
      this.add(splitPane);
      setSize(getSize()); 
      editorPane.setSize(articleView.getViewport().getViewSize());
      validate(); 
      repaint();
  }
  public void setViewTreeOnly()
  {   this.remove(splitPane);
      this.remove(articleView);
      this.add(treeView);
      setSize(getSize());  treeView.setSize(getSize());
      validate(); repaint();
  }
  public void setViewArticleOnly()
  {    this.remove(splitPane);
       this.remove(treeView);
       this.add(articleView); 
       setSize(getSize());  articleView.setSize(getSize()); validate(); repaint();
  }
  /*
  private void setCurrentNode(DefaultMutableTreeNode node)
  {
    setCurrentNode((JreepadNode)(node.getUserObject()));
  }
  */
  private void setCurrentNode(JreepadNode n)
  {
    if(currentNode != null)
    {
      currentNode.setContent(editorPane.getText());
    }
    currentNode = n;
    editorPane.setText(n.getContent());
  }

  public JTree getTree()
  {
    return tree;
  }
  public JreepadNode getRootJreepadNode()
  {
    setCurrentNode(getCurrentNode()); // Ensures any edits have been committed
    return root;
  }
  public JreepadNode getCurrentNode()
  {
    return currentNode;
  }

  public void moveNode(JreepadNode node, JreepadNode newParent)
  {
    // First we need to make sure that the node is not a parent of the new parent
    // - otherwise things would go really wonky!
    if(node.isNodeInSubtree(newParent))
    {
      return;
    }
    JreepadNode oldParent = node.getParentNode();

    // Now make a note of the expanded/collapsed state of the subtree of the moving node
    boolean thisOnesExpanded = tree.isExpanded(tree.getSelectionPath());
    Enumeration enum;
    Vector expanded;
    if(thisOnesExpanded)
    {
      enum = tree.getExpandedDescendants(tree.getSelectionPath());
      expanded = new Vector();
      while(enum.hasMoreElements())
      {
        expanded.add((TreePath)enum.nextElement());
        System.out.println(expanded.lastElement());
      }
    }

    node.removeFromParent();
    newParent.addChild(node);

    treeModel.reload(oldParent);
    treeModel.reload(newParent);
  //  treeModel.reload((TreeNode)tree.getPathForRow(0).getLastPathComponent());
    
    // If the destination node didn't previously have any children, then we'll expand it
 //   if(newParent.getChildCount()==1)
      
    
    // Reapply the expanded/collapsed states
    
  }

  public void indentCurrentNode()
  {
    int nodeRow = tree.getLeadSelectionRow();
    TreePath parentPath = tree.getSelectionPath().getParentPath();
    int pos = currentNode.getIndex();
    if(pos<1) return;
    
    JreepadNode newParent = ((JreepadNode)currentNode.getParent().getChildAt(pos-1));
    
    if(currentNode.indent())
    {
      treeModel.reload(currentNode.getParent().getParent());
      parentPath = parentPath.pathByAddingChild(newParent);
      TreePath myPath = parentPath.pathByAddingChild(currentNode);
      // Now use scrollPathToVisible() or scrollRowToVisible() to make sure it's visible
      tree.scrollPathToVisible(myPath);
      tree.setSelectionPath(myPath);
    }
  }
  public void outdentCurrentNode()
  {
    TreePath parentPath = tree.getSelectionPath().getParentPath();
    if(parentPath==null) return;
    TreePath parentParentPath = parentPath.getParentPath();
    if(parentParentPath==null) return;

    if(currentNode.outdent())
    {
      TreePath myPath = parentParentPath.pathByAddingChild(currentNode);
      treeModel.reload(currentNode.getParent());
      // Now use scrollPathToVisible() or scrollRowToVisible() to make sure it's visible
      tree.scrollPathToVisible(myPath);
      tree.setSelectionPath(myPath);
      System.out.println("New path: " + myPath);
    }
  }

  public void moveCurrentNodeUp()
  {
    TreePath nodePath = tree.getSelectionPath();
    currentNode.moveUp();
    treeModel.reload(currentNode.getParent());
    tree.setSelectionPath(nodePath);
  }
  public void moveCurrentNodeDown()
  {
    TreePath nodePath = tree.getSelectionPath();
    currentNode.moveDown();
    treeModel.reload(currentNode.getParent());
    tree.setSelectionPath(nodePath);
  }
  
  public JreepadNode addNodeAbove()
  {
    int index = currentNode.getIndex();
    if(index==-1)
      return null;
    TreePath parentPath = tree.getSelectionPath().getParentPath();
    JreepadNode parent = currentNode.getParentNode();
    JreepadNode ret = parent.addChild(index);
    treeModel.nodesWereInserted(parent, new int[]{index});
    tree.startEditingAtPath(parentPath.pathByAddingChild(ret));
    return ret;
  }
  public JreepadNode addNodeBelow()
  {
    int index = currentNode.getIndex();
    if(index==-1)
      return null;
    TreePath parentPath = tree.getSelectionPath().getParentPath();
    JreepadNode parent = currentNode.getParentNode();
    JreepadNode ret = parent.addChild(index+1);
    treeModel.nodesWereInserted(parent, new int[]{index+1});
    tree.startEditingAtPath(parentPath.pathByAddingChild(ret));
    return ret;
  }
  public JreepadNode addNode()
  {
    JreepadNode ret = currentNode.addChild();
    TreePath nodePath = tree.getSelectionPath();
    treeModel.nodesWereInserted(currentNode, new int[]{currentNode.getIndex(ret)});
    tree.startEditingAtPath(nodePath.pathByAddingChild(ret));
    return ret;
  }
  public JreepadNode removeNode()
  {
    JreepadNode parent = (JreepadNode)currentNode.getParent();
    TreePath parentPath = tree.getSelectionPath().getParentPath();
    if(parent != null)
    {
      int index = parent.getIndex(currentNode);
      JreepadNode ret = parent.removeChild(index);
      setCurrentNode(parent);

      tree.setSelectionPath(parentPath);
      treeModel.nodesWereRemoved(parent, new int[]{index}, new Object[]{ret});

      repaint();
      return ret;
    }
    else
      return null;
  }

  public void sortChildren()
  {
    currentNode.sortChildren();
    treeModel.reload(currentNode);
    // System.out.println(currentNode.toFullString());
  }
  public void sortChildrenRecursive()
  {
    currentNode.sortChildrenRecursive();
    treeModel.reload(currentNode);
    // System.out.println(currentNode.toFullString());
  }
  
  public void returnFocusToTree()
  {
    tree.requestFocus();
  }

  // Functions and inner class for searching nodes
  private JreepadSearchResult[] searchResults;
  private Vector searchResultsVec;
  private Object foundObject;
  public boolean performSearch(String inNodes, String inArticles, int searchWhat /* 0=selected, 1=all */,
  							boolean orNotAnd, boolean caseSensitive, int maxResults)
  {
    searchResults = null;
    searchResultsVec = new Vector();
    
    // Now look through the nodes, adding things to searchResultsVec if found.
    switch(searchWhat)
    {
      case 0: // search selected node
        recursiveSearchNode(inNodes, inArticles, currentNode, tree.getSelectionPath(), orNotAnd, caseSensitive, maxResults);
        break;
      default: // case 1==search whole tree
        recursiveSearchNode(inNodes, inArticles, root, new TreePath(root), orNotAnd, caseSensitive, maxResults);
        break;
    }

    if(searchResultsVec.size()>0)
    {
      searchResults = new JreepadSearchResult[searchResultsVec.size()];
      for(int i=0; i<searchResults.length; i++)
      {
        foundObject = searchResultsVec.get(i);
        searchResults[i] = (JreepadSearchResult)foundObject;
      }
      return true;
    }
    return false;
  }
  private static final int articleQuoteMaxLen = 40;
  private void recursiveSearchNode(String inNodes, String inArticles, JreepadNode thisNode, TreePath pathSoFar,
  					boolean orNotAnd, boolean caseSensitive, int maxResults)
  {
    if(searchResultsVec.size()>=maxResults) return;
    
    String quoteText;
    
    // These things ensure case-sensitivity behaves
    String casedInNodes = caseSensitive    ? inNodes               : inNodes.toUpperCase();
    String casedInArticles = caseSensitive ? inArticles            : inArticles.toUpperCase();
    String casedNode = caseSensitive       ? thisNode.getTitle()   : thisNode.getTitle().toUpperCase();
    String casedArticle = caseSensitive    ? thisNode.getContent() : thisNode.getContent().toUpperCase();
    
    // Look in current node. If it matches criteria, add "pathSoFar" to the Vector
    boolean itMatches;
    boolean nodeMatches    = inNodes.equals("")    || casedNode.indexOf(casedInNodes)!=-1;
    boolean articleMatches = inArticles.equals("") || casedArticle.indexOf(casedInArticles)!=-1;


    if(inNodes.equals("") && inArticles.equals(""))
      itMatches = false;
    else if(inNodes.equals("")) // Only looking in articles
      itMatches = articleMatches;
    else if(inArticles.equals("")) // Only looking in nodes
      itMatches = nodeMatches;
    else // Looking in both
      if(orNotAnd) // Use OR combinator
        itMatches = nodeMatches || articleMatches;
      else // Use AND combinator
        itMatches = nodeMatches && articleMatches;


    if(itMatches)
    {
      if(!articleMatches)
      {
        if(thisNode.getContent().length()>articleQuoteMaxLen)
          quoteText = thisNode.getContent().substring(0,articleQuoteMaxLen) + "...";
        else
          quoteText = thisNode.getContent();
      }
      else
      {
        quoteText = "";
        int start = casedArticle.indexOf(casedInArticles);
        String substring;
        if(start>0)
          quoteText += "...";
        else
          start = 0;
        substring = thisNode.getContent();
        if(substring.length() > articleQuoteMaxLen)
          quoteText += substring.substring(0,articleQuoteMaxLen) + "...";
        else
          quoteText += thisNode.getContent().substring(start);
      }
      searchResultsVec.add(new JreepadSearchResult(pathSoFar, quoteText, thisNode));
//      System.out.println("Positive match: "+thisNode);
    }
    
    // Whether or not it matches, make the recursive call on the children
    Enumeration getKids = thisNode.children();
    JreepadNode thisKid;
    while(getKids.hasMoreElements())
    {
      thisKid = (JreepadNode)getKids.nextElement();
      recursiveSearchNode(inNodes, inArticles, thisKid, pathSoFar.pathByAddingChild(thisKid), 
                          orNotAnd, caseSensitive, maxResults);
    }
  }
  public JreepadSearchResult[] getSearchResults()
  {
    return searchResults;
  }
  public class JreepadSearchResult
  {
    private TreePath treePath;
    private String articleQuote;
    private JreepadNode node;
    public JreepadSearchResult(TreePath treePath, String articleQuote, JreepadNode node)
    {
      this.treePath = treePath;
      this.articleQuote = articleQuote;
      this.node = node;
    }
    public String getArticleQuote()	{ return articleQuote;	}
    public TreePath getTreePath()	{ return treePath;		}
    public JreepadNode getNode()	{ return node;		}
  }
  // End of: functions and inner class for searching nodes

  public void addChildrenFromTextFiles(File[] inFiles) throws IOException
  {
	for(int i=0; i<inFiles.length; i++)
      getCurrentNode().addChildFromTextFile(inFiles[i]);
    treeModel.reload(currentNode);
    tree.expandPath(tree.getSelectionPath());
  }
  
  public void addChild(JreepadNode newKid)
  {
	getCurrentNode().addChild(newKid);
    treeModel.reload(currentNode);
    tree.expandPath(tree.getSelectionPath());
  }

  class JreepadTreeModelListener implements TreeModelListener
  {
    public void treeNodesChanged(TreeModelEvent e)
    {
      Object[] parentPath = e.getPath(); // Parent of the changed node(s)
      int[] children = e.getChildIndices(); // Indices of the changed node(s)
      JreepadNode parent = (JreepadNode)(parentPath[parentPath.length-1]);
      tree.repaint();
    }
    public void treeNodesInserted(TreeModelEvent e)
    {
      Object[] parentPath = e.getPath(); // Parent of the new node(s)
      int[] children = e.getChildIndices(); // Indices of the new node(s)
      JreepadNode parent = (JreepadNode)(parentPath[parentPath.length-1]);
      tree.expandPath(e.getTreePath());
      tree.scrollPathToVisible(e.getTreePath());
      tree.repaint();
    }
    public void treeNodesRemoved(TreeModelEvent e)
    {
      Object[] parentPath = e.getPath(); // Parent of the removed node(s)
      int[] children = e.getChildIndices(); // Indices the node(s) had before they were removed
      JreepadNode parent = (JreepadNode)(parentPath[parentPath.length-1]);
      tree.repaint();
    }
    public void treeStructureChanged(TreeModelEvent e)
    {
      Object[] parentPath = e.getPath(); // Parent of the changed node(s)
      JreepadNode parent = (JreepadNode)(parentPath[parentPath.length-1]);
      tree.repaint();
    }
  } // End of: class JreepadTreeModelListener

}