/**
 * Tree control
 *
 * @author Shane Bryzak
 */
 
//Package("xw.controls");
xw = new Object();
xw.controls = new Object();

// TODO - fully qualified naming is way too verbose, make it shorter somehow

xw.controls.TreeModel = function(rootNode)
{
  this.rootNode = rootNode;
  this.rootNode.setModel(this);
  this.tree = null;
  var p = xw.controls.TreeModel.prototype;
  
  p.getChild = function(parent, index)
  {
    return parent.getChildAt(index);
  }

  p.getChildCount = function(parent)
  {
    return parent.getChildCount();
  }

  p.getIndexOfChild = function(parent, child)
  {
    return parent.getIndex(child);
  }

  p.getRoot = function()
  {
    return this.rootNode;
  }

  p.isLeaf = function(node)
  {
    return node.isLeaf();
  }

  p.setTree = function(tree)
  {
    this.tree = tree;
  }

  p.findNodeByObject = function(object)
  {
    return xw.controls.TreeModel.searchChildrenForObject(this.rootNode, object);
  }

  xw.controls.TreeModel.searchChildrenForObject = function(node, object)
  {
    for (var i = 0; i < node.getChildCount(); i++)
    {
      var childNode = node.getChildAt(i);
      if (childNode.getUserObject() == object)
        return childNode;
      else if (childNode.getChildCount() > 0)
      {
        grandChild = xw.controls.TreeModel.searchChildrenForObject(childNode, object);
        if (grandChild != null)
          return grandChild;
      }
    }
    return null;
  }
}

xw.controls.TreeNode = function(value, leaf, userObject)
{
  this.value = value;
  this.leaf = leaf ? leaf : false;
  this.userObject = userObject ? userObject : null;
  this.children = new Array();
  this.parent = null;
  this.expanded = false;
  this.model = null;

  xw.controls.TreeNode.prototype.getUserObject = function()
  {
    return this.userObject;
  }

  xw.controls.TreeNode.prototype.setUserObject = function(obj)
  {
    this.userObject = obj;
  }

  xw.controls.TreeNode.prototype.setModel = function(model)
  {
    this.model = model;
  }

  xw.controls.TreeNode.prototype.add = function(node)
  {
    node.parent = this;
    this.children.push(node);
    node.setModel(this.model);
  }

  xw.controls.TreeNode.prototype.children = function()
  {
    return this.children;
  }

  xw.controls.TreeNode.prototype.getChildAt = function(index)
  {
    return this.children[index];
  }

  xw.controls.TreeNode.prototype.getChildCount = function()
  {
    return this.children.length;
  }

  xw.controls.TreeNode.prototype.getIndex = function(node)
  {
    for (var i = 0; i < this.children.length; i++)
    {
      if (this.children[i] == node)
        return i;
    }
    return -1;
  }

  xw.controls.TreeNode.prototype.getParent = function()
  {
    return this.parent;
  }

  xw.controls.TreeNode.prototype.isLeaf = function()
  {
    return this.leaf;
  }

  xw.controls.TreeNode.prototype.remove = function(node)
  {
    var found = false;
    for (var i = 0; i < this.children.length; i++)
    {
      if (this.children[i] == node)
        found = true;
      if (found && i < this.children.length - 1)
        this.children[i] = this.children[i + 1];
    }
    if (found)
    {
      this.children.length = this.children.length - 1;
      this.model.tree.renderer.removeNode(node);
    }
  }
}

xw.controls.Tree = function(container, model)
{
  this.container = (typeof(container) == "object") ? container : document.getElementById(container);
  this.rootVisible = false;
  this.model = model;
  model.setTree(this);
  this.renderer = new xw.controls.DefaultCellRenderer();
  this.onSelect = null;
  this.onDragDrop = null;
  this.selectedNode = null;

  xw.controls.Tree.prototype.isRootVisible = function()
  {
    return this.rootVisible;
  }

  xw.controls.Tree.prototype.setRootVisible = function(visible)
  {
    this.rootVisible = visible;
  }

  xw.controls.Tree.prototype.paint = function()
  {
    this.renderer.render(this.container, this.model.getRoot(), true);
  }

  xw.controls.Tree.prototype.repaintNode = function(node)
  {
    this.renderer.render(null, node, true);
  }

  xw.controls.Tree.prototype.getModel = function()
  {
    return this.model;
  }

  xw.controls.Tree.prototype.selectNode = function(node)
  {
    if (this.selectedNode)
      this.renderer.renderSelected(this.selectedNode, false);
    this.selectedNode = node;
    this.renderer.renderSelected(node, true);
    if (this.onSelect)
      this.onSelect(node);
  }

  xw.controls.Tree.prototype.initiateDragDrop = function(sourceNode, targetNode)
  {
    if ((this.onDragDrop && this.onDragDrop(sourceNode, targetNode)) || !this.onDragDrop)
        this.moveNode(sourceNode, targetNode);
  }

  xw.controls.Tree.prototype.moveNode = function(sourceNode, targetNode)
  {
    var sourceParent = sourceNode.getParent();
    if (sourceParent != targetNode)
    {
      sourceParent.remove(sourceNode);

      targetNode.add(sourceNode);

      targetNode.childrenCell.appendChild(sourceNode.tableCtl);
      this.repaintNode(sourceParent);

      targetNode.expanded = true;
      this.repaintNode(targetNode);
    }
  }
}

xw.controls.Tree.mouseDownNode = null;
xw.controls.Tree.draggedNode = null;
xw.controls.Tree.mouseDownStartPos = null;
xw.controls.Tree.dragThreshold = 5;
xw.controls.Tree.dragDiv = null;
xw.controls.Tree.targetNode = null;

xw.controls.Tree.util = new Object();
xw.controls.Tree.util.addEvent = function(ctl, event, func)
{
  if (navigator.userAgent.indexOf("MSIE") != -1)
    ctl.attachEvent("on" + event, func);
  else
    ctl.addEventListener(event, func, true);
}

xw.controls.Tree.util.removeEvent = function(ctl, event, func)
{
  if (navigator.userAgent.indexOf("MSIE") != -1)
    ctl.detachEvent("on" + event, func);
  else
    ctl.removeEventListener(event, func, true);
}

xw.controls.Tree.util.cancelEventBubble = function(event)
{
  if (navigator.userAgent.indexOf("MSIE") != -1)
  {
    window.event.cancelBubble = true;
    window.event.returnValue = false;
  }
  else
    event.preventDefault();
}

xw.controls.Tree.util.setOpacity = function(ctl, percent)
{
  if (navigator.userAgent.indexOf("MSIE") != -1) 
    ctl.style.filter = "alpha(opacity=" + percent + ")";
  else
    ctl.style.MozOpacity = percent / 100;
}

xw.controls.Tree.util.fades = new Array();
xw.controls.Tree.util.fading = false;

xw.controls.Tree.util.startFade = function(fade)
{
  fade.valid = true;

  if (fade.value == 0)
    fade.control.style.display = "";

  xw.controls.Tree.util.fades.push(fade);

  if (!xw.controls.Tree.util.fading)
    xw.controls.Tree.util.processFades();
}

xw.controls.Tree.util.fadeIn = function(ctl, step)
{
  xw.controls.Tree.util.setOpacity(ctl, 0);
  xw.controls.Tree.util.startFade({control:ctl,value:0,step:step});
}

xw.controls.Tree.util.fadeOut = function(ctl, step, onComplete)
{
  xw.controls.Tree.util.setOpacity(ctl, 100);
  xw.controls.Tree.util.startFade({control:ctl,value:100,step:-1 * step,onComplete:onComplete});
}

xw.controls.Tree.util.processFades = function()
{
  xw.controls.Tree.util.fading = true;

  for (var i = 0; i < xw.controls.Tree.util.fades.length; i++)
  {
    var fade = xw.controls.Tree.util.fades[i];
    var done = false;

    if (fade.step < 0) // Fade out
    {
      xw.controls.Tree.util.setOpacity(fade.control, Math.max(fade.value, 0));
      if (fade.value < 0)
        done = true;
    }
    else if (fade.step > 0) // Fade in
    {
      xw.controls.Tree.util.setOpacity(fade.control, Math.min(fade.value, 100));
      if (fade.value > 100)
        done = true;
    }

    if (done)
    {
      xw.controls.Tree.util.fades.splice(i, 1);
      if (fade.onComplete)
        fade.onComplete();
    }
    fade.value += fade.step;
  }

  if (xw.controls.Tree.util.fades.length > 0)
    setTimeout("xw.controls.Tree.util.processFades()", 50);
  else
    xw.controls.Tree.util.fading = false;
}

xw.controls.Tree.util.getMousePos = function(event)
{
  var x, y;
  if (navigator.userAgent.indexOf("MSIE") != -1)
  {
    x = window.event.clientX + document.documentElement.scrollLeft + document.body.scrollLeft;
    y = window.event.clientY + document.documentElement.scrollTop + document.body.scrollTop;
  }
  else
  {
    x = event.clientX + window.scrollX;
    y = event.clientY + window.scrollY;
  }
  return { x:x, y:y };
};

xw.controls.Tree.util.calcDistance = function(pos1, pos2)
{
  var deltaX = Math.abs(pos1.x - pos2.x);
  var deltaY = Math.abs(pos1.y - pos2.y);
  return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
}

xw.controls.Tree.onMouseDown = function(event, node)
{
  xw.controls.Tree.mouseDownStartPos = xw.controls.Tree.util.getMousePos(event);
  xw.controls.Tree.mouseDownNode = node;
  xw.controls.Tree.util.addEvent(document, "mousemove", xw.controls.Tree.onMouseMove);
  xw.controls.Tree.util.addEvent(document, "mouseup", xw.controls.Tree.onMouseUp);

  xw.controls.Tree.util.cancelEventBubble(event);
}

xw.controls.Tree.onMouseMove = function(event)
{
  if (xw.controls.Tree.draggedNode == null)
  {
    var distance = xw.controls.Tree.util.calcDistance(xw.controls.Tree.util.getMousePos(event), xw.controls.Tree.mouseDownStartPos);
    if (distance > xw.controls.Tree.dragThreshold)
    {
      xw.controls.Tree.draggedNode = xw.controls.Tree.mouseDownNode;
      if (xw.controls.Tree.dragDiv == null)
      {
        xw.controls.Tree.targetNode = null;
        xw.controls.Tree.dragDiv = document.createElement("div");
        xw.controls.Tree.dragDiv.style.position = "absolute";
        xw.controls.Tree.util.setOpacity(xw.controls.Tree.dragDiv, 40);
        window.document.body.appendChild(xw.controls.Tree.dragDiv);
      }

      xw.controls.Tree.draggedNode.renderer.renderClone(xw.controls.Tree.dragDiv, xw.controls.Tree.draggedNode);
      xw.controls.Tree.dragDiv.style.display = "";
      var pos = xw.controls.Tree.util.getMousePos(event);
      xw.controls.Tree.dragDiv.style.left = (pos.x + 10) + "px";
      xw.controls.Tree.dragDiv.style.top = pos.y + "px";
    }
  }
  else
  {
    var pos = xw.controls.Tree.util.getMousePos(event);
    xw.controls.Tree.dragDiv.style.left = (pos.x + 10) + "px";
    xw.controls.Tree.dragDiv.style.top = pos.y + "px";
  }
}

xw.controls.Tree.onMouseUp = function(event)
{
  xw.controls.Tree.util.removeEvent(document, "mousemove", xw.controls.Tree.onMouseMove);
  xw.controls.Tree.util.removeEvent(document, "mouseup", xw.controls.Tree.onMouseUp);

  xw.controls.Tree.mouseDownStartPos = null;

  if (xw.controls.Tree.dragDiv)
    xw.controls.Tree.dragDiv.style.display = "none";

  if (xw.controls.Tree.targetNode)
  {
    xw.controls.Tree.targetNode.renderer.renderSelected(xw.controls.Tree.targetNode, false);
    xw.controls.Tree.draggedNode.model.tree.initiateDragDrop(xw.controls.Tree.draggedNode, xw.controls.Tree.targetNode);
  }
  else if (xw.controls.Tree.mouseDownNode.model && xw.controls.Tree.mouseDownNode.model.tree && !xw.controls.Tree.draggedNode)
    xw.controls.Tree.mouseDownNode.model.tree.selectNode(xw.controls.Tree.mouseDownNode);

  xw.controls.Tree.targetNode = null;
  xw.controls.Tree.draggedNode = null;

  xw.controls.Tree.util.cancelEventBubble(event);
}

xw.controls.Tree.onMouseOver = function(event, node)
{
  if (xw.controls.Tree.draggedNode && xw.controls.Tree.draggedNode != node && !node.isLeaf() && xw.controls.Tree.draggedNode.getParent() != node)
  {
    node.renderer.renderSelected(node, true);
    xw.controls.Tree.targetNode = node;
  }
}

xw.controls.Tree.onMouseOut = function(event, node)
{
  if (xw.controls.Tree.draggedNode && xw.controls.Tree.draggedNode != node && !node.isLeaf())
  {
    node.renderer.renderSelected(node, false);
    if (xw.controls.Tree.targetNode == node)
      xw.controls.Tree.targetNode = null;
  }
}

xw.controls.DefaultCellRenderer = function()
{
  this.plusStartClass = "treePlusStart";
  this.plusMiddleClass = "treePlusMiddle";
  this.plusEndClass = "treePlusEnd";
  this.plusNoneClass = "treePlusNone";

  this.minusStartClass = "treeMinusStart";
  this.minusMiddleClass = "treeMinusMiddle";
  this.minusEndClass = "treeMinusEnd";
  this.minusNoneClass = "treeMinusNone";

  this.lineMiddleClass = "treeLineMiddle";
  this.lineEndClass = "treeLineEnd";
  this.lineBranchClass = "treeLineBranch";

  this.leafClass = "treeLeaf";
  this.folderOpenClass = "treeFolderOpen";
  this.folderClosedClass = "treeFolderClosed";

  this.onRender = false;

  xw.controls.DefaultCellRenderer.prototype.removeNode = function(node)
  {
    node.parent.childrenCell.removeChild(node.tableCtl);
  }

  xw.controls.DefaultCellRenderer.prototype.render = function(container, node, renderChildren)
  {
    if (!node.tableCtl)
    {
      node.renderer = this;

      node.tableCtl = document.createElement("table");
      node.tableCtl.cellSpacing = 0;
      node.tableCtl.cellPadding = 0;

      node.headerRow = node.tableCtl.insertRow(-1);

      node.branchCell = node.headerRow.insertCell(-1);
      node.iconCell = node.headerRow.insertCell(-1);
      node.contentCell = node.headerRow.insertCell(-1);

      node.contentCell.style.textAlign = "left";
      node.contentCell.style.whiteSpace = "nowrap";
      node.contentCell.style.cursor = "pointer";
      node.contentCell.style.verticalAlign = "middle";

      node.branchDiv = document.createElement("div");

      var toggleFunction = function(event) { node.expanded = !node.expanded; node.renderer.toggle(node); };

      xw.controls.Tree.util.addEvent(node.branchDiv, "mousedown", toggleFunction);

      node.branchCell.appendChild(node.branchDiv);

      node.iconDiv = document.createElement("div");
      node.iconDiv.style.position = "static";

      node.iconCell.style.width = "1px";
      node.iconCell.appendChild(node.iconDiv);

      node.content = document.createElement("span");
      node.content.className = "unselected";
      node.contentText = document.createTextNode(node.value);
      node.content.appendChild(node.contentText);

      node.contentCell.appendChild(node.content);

      node.childrenRow = node.tableCtl.insertRow(-1);
      node.childBranchCell = node.childrenRow.insertCell(-1);

      node.childBranchDiv = document.createElement("div");
      node.childBranchCell.appendChild(node.childBranchDiv);

      node.childrenCell = node.childrenRow.insertCell(-1);
      node.childrenCell.colSpan = 2;

      var mouseDownFunction = function(event) { xw.controls.Tree.onMouseDown(event, node); };
      xw.controls.Tree.util.addEvent(node.iconDiv, "mousedown", mouseDownFunction);
      xw.controls.Tree.util.addEvent(node.contentCell, "mousedown", mouseDownFunction);

      var mouseOverFunction = function(event) { xw.controls.Tree.onMouseOver(event, node); };
      xw.controls.Tree.util.addEvent(node.iconDiv, "mouseover", mouseOverFunction);
      xw.controls.Tree.util.addEvent(node.contentCell, "mouseover", mouseOverFunction);

      var mouseOutFunction = function(event) { xw.controls.Tree.onMouseOut(event, node); };
      xw.controls.Tree.util.addEvent(node.iconDiv, "mouseout", mouseOutFunction);
      xw.controls.Tree.util.addEvent(node.contentCell, "mouseout", mouseOutFunction);
    }

    node.contentText.nodeValue = node.value;

    if (container)
    {
      var inContainer = false;
      for (var i = 0; i < container.childNodes.length; i++)
      {
        if (container.childNodes[i] == node.tableCtl)
        {
          inContainer = true;
          break;
        }
      }

      if (!inContainer)
        container.appendChild(node.tableCtl);
    }

    if (!node.isLeaf() && renderChildren)
    {
      for (var i = 0; i < node.getChildCount(); i++)
        this.render(node.childrenCell, node.getChildAt(i), true);
    }

    // Reset the child branch div height
    node.childBranchDiv.style.height = "100%";

    var expanded = node.expanded && (node.getChildCount() > 0);
//    node.childrenRow.style.display = expanded ? "" : "none";

    if (node.isLeaf())
    {
      if (node.getParent() == null)
        node.branchDiv.className = this.lineBranchClass;
      else if (node.getParent().getIndex(node) == node.getParent().getChildCount() - 1)
        node.branchDiv.className = this.lineEndClass;
      else
        node.branchDiv.className = this.lineBranchClass;

      node.iconDiv.className = this.leafClass;
    }
    else
    {
      if (node.getParent() == null)
      {
        if (node.getChildCount() > 0)
          node.branchDiv.className = expanded ? this.minusNoneClass : this.plusNoneClass;
        else
          node.branchDiv.className = "";
      }
      else if (node.getParent().getIndex(node) == node.getParent().getChildCount() - 1)
      {
        if (node.getChildCount() > 0)
        {
          node.branchDiv.className = expanded ? this.minusEndClass : this.plusEndClass;
          node.childBranchDiv.className = "";
          node.childBranchDiv.style.width = "100%";
        }
        else
          node.branchDiv.className = this.lineEndClass;
      }
      else
      {
        if (node.getChildCount() > 0)
        {
          node.childBranchDiv.className = this.lineMiddleClass;
          node.childBranchDiv.style.height = node.childBranchCell.offsetHeight + "px";

          node.branchDiv.className = expanded ? this.minusMiddleClass : this.plusMiddleClass;
        }
        else
          node.branchDiv.className = this.lineBranchClass;
      }
      node.iconDiv.className = expanded ? this.folderOpenClass : this.folderClosedClass;
    }

    if (node.getParent())
      node.renderer.render(null, node.getParent());

    if (this.onRender)
      this.onRender(node);

    node.childrenRow.style.display = expanded ? "" : "none";
  }

  xw.controls.DefaultCellRenderer.prototype.toggle = function(node)
  {
    if (node.expanded)
    {
      // Reset the child branch div height
      node.childrenRow.style.display = "";
      xw.controls.Tree.util.fadeIn(node.childrenCell, 25);
      this.decorateNode(node, true);
    }
    else
    {
      this.decorateNode(node, false);
      var onComplete = function() { node.childrenRow.style.display = "none"; node.renderer.decorateNode(node, true); };
      xw.controls.Tree.util.fadeOut(node.childrenCell, 34, onComplete);
    }
  }

  xw.controls.DefaultCellRenderer.prototype.decorateNode = function(node, recurseUp)
  {
    node.childBranchDiv.style.height = "0px";

    if (node.isLeaf())
    {
      if (node.getParent() == null)
        node.branchDiv.className = this.lineBranchClass;
      else if (node.getParent().getIndex(node) == node.getParent().getChildCount() - 1)
        node.branchDiv.className = this.lineEndClass;
      else
        node.branchDiv.className = this.lineBranchClass;

      node.iconDiv.className = this.leafClass;
    }
    else
    {
      if (node.getParent() == null)
      {
        if (node.getChildCount() > 0)
          node.branchDiv.className = node.expanded ? this.minusNoneClass : this.plusNoneClass;
        else
          node.branchDiv.className = "";
      }
      else if (node.getParent().getIndex(node) == node.getParent().getChildCount() - 1)
      {
        if (node.getChildCount() > 0)
        {
          node.branchDiv.className = node.expanded ? this.minusEndClass : this.plusEndClass;
          node.childBranchDiv.className = "";
          node.childBranchDiv.style.width = "100%";
        }
        else
          node.branchDiv.className = this.lineEndClass;
      }
      else
      {
        if (node.getChildCount() > 0)
        {
          node.childBranchDiv.className = this.lineMiddleClass;
          node.childBranchDiv.style.height = node.childBranchCell.offsetHeight + "px";

          node.branchDiv.className = node.expanded ? this.minusMiddleClass : this.plusMiddleClass;
        }
        else
          node.branchDiv.className = this.lineBranchClass;
      }
      node.iconDiv.className = node.expanded ? this.folderOpenClass : this.folderClosedClass;
    }

    if (node.getParent() && recurseUp)
      node.renderer.decorateNode(node.getParent(), true);
  }

  xw.controls.DefaultCellRenderer.prototype.renderSelected = function(node, selected)
  {
    node.content.className = selected ? "selected" : "unselected";
  }

  xw.controls.DefaultCellRenderer.prototype.renderClone = function(container, node)
  {
    var tbl = document.createElement("table");
    tbl.cellSpacing = 0;
    tbl.cellPadding = 0;
    var row = tbl.insertRow(-1);
    row.appendChild(node.iconCell.cloneNode(true));
    row.appendChild(node.contentCell.cloneNode(true));

    if (container.firstChild)
      container.replaceChild(tbl, container.firstChild);
    else
      container.appendChild(tbl);
  }
}
