package astah.plugin

import com.change_vision.jude.api.inf.model.IAttribute
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView
import com.change_vision.jude.api.inf.ui.ISelectionListener
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener
import com.change_vision.jude.api.inf.view.IEntitySelectionEvent
import com.change_vision.jude.api.inf.view.IEntitySelectionListener
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class InstanceOfView: JPanel(), IPluginExtraTabView, ProjectEventListener, IEntitySelectionListener,
    IDiagramEditorSelectionListener, ActionListener {
    val buttonMakeInstance = ButtonMakeInstance()
    val buttonInheritFromSuperclasses = ButtonInheritFromSuperclasses()
    val buttonMakeInstanceOfRelation = ButtonMakeInstanceOfRelation()
    val buttonRefreshInstances = JButton("Refresh Instances")
    val buttonSelectType = JButton("Select Type")
    val buttonSelectValue = JButton("Select Value")
    val buttonGoToOccurrence = JButton("Go to Occurrence")
    val classes = mutableMapOf<IClass, Pair<List<IClass>, List<IClass>>>()
    val subclassListModel = DefaultListModel<IClass>()
    val subclassList =  JList(subclassListModel)
    val instanceListModel = DefaultListModel<String>()
    val instanceList =  JList(instanceListModel)
    val occurrenceListModel = DefaultListModel<IClass>()
    val occurrenceList = JList(occurrenceListModel)
    var selectedAttribute: IAttribute? = null

    val panelButtons = JPanel()
    val panelSelectType = JPanel()
    val panelSelectValue = JPanel()
    val panelOccurrences = JPanel()
    val panel = JPanel()


    private fun initButtons() {
        panelButtons.layout = GridLayout(4,1)
        panelButtons.add(buttonMakeInstance)
        panelButtons.add(buttonInheritFromSuperclasses)
        panelButtons.add(buttonMakeInstanceOfRelation)
        panelButtons.add(buttonRefreshInstances)
        buttonMakeInstance.addActionListener(this)
        buttonInheritFromSuperclasses.addActionListener(this)
        buttonMakeInstanceOfRelation.addActionListener(this)
        buttonRefreshInstances.addActionListener(this)
        panel.add(panelButtons)
    }

    private fun initSelectType() {
        panelSelectType.layout = BoxLayout(panelSelectType, BoxLayout.PAGE_AXIS)
        val selectionModelForSubclasses = DefaultListSelectionModel()
        selectionModelForSubclasses.selectionMode = ListSelectionModel.SINGLE_SELECTION
        instanceList.selectionModel = selectionModelForSubclasses
        val descriptionSelectType = JLabel("Possible types")
        val subclassScroller = JScrollPane(subclassList)
        subclassScroller.preferredSize = Dimension(180, 120)
        descriptionSelectType.alignmentX = Component.CENTER_ALIGNMENT
        buttonSelectType.alignmentX = Component.CENTER_ALIGNMENT
        subclassScroller.alignmentX = Component.CENTER_ALIGNMENT
        panelSelectType.add(descriptionSelectType)
        panelSelectType.add(subclassScroller)
        panelSelectType.add(buttonSelectType)
        buttonSelectType.addActionListener(this)
        panel.add(panelSelectType)
    }

    private fun initSelectValue() {
        panelSelectValue.layout = BoxLayout(panelSelectValue, BoxLayout.PAGE_AXIS)
        val selectionModelForInstance = DefaultListSelectionModel()
        selectionModelForInstance.selectionMode = ListSelectionModel.SINGLE_SELECTION
        instanceList.selectionModel = selectionModelForInstance
        val descriptionSelectValue = JLabel("Defined instances")
        val instanceScroller = JScrollPane(instanceList)
        instanceScroller.preferredSize = Dimension(180, 120)
        descriptionSelectValue.alignmentX = Component.CENTER_ALIGNMENT
        buttonSelectValue.alignmentX = Component.CENTER_ALIGNMENT
        instanceScroller.alignmentX = Component.CENTER_ALIGNMENT
        panelSelectValue.add(descriptionSelectValue)
        panelSelectValue.add(instanceScroller)
        panelSelectValue.add(buttonSelectValue)
        buttonSelectValue.addActionListener(this)
        panel.add(panelSelectValue)
    }

    private fun initOccurrences() {
        panelOccurrences.layout = BoxLayout(panelOccurrences, BoxLayout.PAGE_AXIS)
        val selectionModelForInstance = DefaultListSelectionModel()
        selectionModelForInstance.selectionMode = ListSelectionModel.SINGLE_SELECTION
        occurrenceList.selectionModel = selectionModelForInstance
        val descriptionOccurrence = JLabel("Occurrences")
        val occurrenceScroller = JScrollPane(occurrenceList)
        occurrenceScroller.preferredSize = Dimension(180, 120)
        descriptionOccurrence.alignmentX = Component.CENTER_ALIGNMENT
        buttonGoToOccurrence.alignmentX = Component.CENTER_ALIGNMENT
        occurrenceScroller.alignmentX = Component.CENTER_ALIGNMENT
        panelOccurrences.add(descriptionOccurrence)
        panelOccurrences.add(occurrenceScroller)
        panelOccurrences.add(buttonGoToOccurrence)
        buttonGoToOccurrence.addActionListener(this)
        panel.add(panelOccurrences)
    }

    init {
        panel.layout = GridLayout(1,3)
        initButtons()
        initSelectType()
        initSelectValue()
        initOccurrences()
        add(panel)
        AstahAccessor.addProjectEventListener(this)
        AstahAccessor.addIDiagramEditorSelectionListener(this)
        AstahAccessor.addEntitySelectionListener(this)
    }

    private fun getChildren(clazz: IClass): Pair<List<IClass>, List<IClass>> {
        val subClasses = mutableListOf<IClass>()
        val instances = mutableListOf<IClass>()
        fun getChildren1(clazz1: IClass, rootClass: MutableList<IClass>) {
            rootClass += clazz1
            val ret1 = getChildren(clazz1)
            subClasses += ret1.first
            instances += ret1.second
        }
        AstahAccessor.getDirectSubClasses(clazz).forEach { getChildren1(it, subClasses) }
        InstanceOf.getInstances(clazz).forEach { getChildren1(it, instances) }
        return Pair(subClasses, instances)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            buttonMakeInstance -> buttonMakeInstance.push(AstahAccessor.getCurrentDiagram())
            buttonInheritFromSuperclasses -> buttonInheritFromSuperclasses.push(AstahAccessor.getCurrentDiagram())
            buttonMakeInstanceOfRelation -> buttonMakeInstanceOfRelation.push(AstahAccessor.getCurrentDiagram())
            buttonSelectType -> {
                subclassList.selectedValuesList.firstOrNull()?.let { type ->
                    selectedAttribute?.let { attribute ->
                        AstahAccessor.setType(attribute, type)
                        updateSelections()
                    }
                }
            }
            buttonSelectValue -> {
                instanceList.selectedValuesList.firstOrNull()?.let { value ->
                    selectedAttribute?.let { attribute ->
                        AstahAccessor.setInitialValue(attribute, value)
                    }
                }
            }
            buttonRefreshInstances -> {
                initialize()
            }
            buttonGoToOccurrence -> {
                occurrenceList.selectedValue?.let { value ->
                    value.presentations.firstOrNull()?.let { presentation ->
                        AstahAccessor.selectDiagram(presentation.diagram)
                        AstahAccessor.showInDiagramEditor(presentation)
                    }
                }
            }
        }
    }

    override fun getTitle(): String = "Instance-Of"
    override fun getDescription(): String = "Instance-Of"
    override fun getComponent(): Component = this
    override fun addSelectionListener(p0: ISelectionListener?) = Unit

    private fun initialize() {
        classes.clear()
        AstahAccessor.getAllClasses().forEach { clazz ->
            val instances = mutableListOf<IClass>()
            val ret = getChildren(clazz)
            instances += ret.first
            instances += ret.second
            val subclasses = AstahAccessor.getAllSubclasses(clazz)
            classes[clazz] = Pair(subclasses, instances)
        }
    }

    override fun activated() = initialize()
    override fun deactivated() = Unit

    override fun projectOpened(p0: ProjectEvent?) {
        subclassListModel.clear()
        instanceListModel.clear()
        occurrenceListModel.clear()
        initialize()
    }
    override fun projectClosed(p0: ProjectEvent?) = Unit
    override fun projectChanged(p0: ProjectEvent?) = Unit
    override fun diagramSelectionChanged(p0: IDiagramEditorSelectionEvent?) = Unit

    private fun updateSelections() {
        val selectedElements = AstahAccessor.getSelectedElements()
        subclassListModel.clear()
        instanceListModel.clear()
        occurrenceListModel.clear()
        selectedElements?.firstOrNull()?.let { element ->
            if (element is IAttribute) {
                element.type?.let { type ->
                    selectedAttribute = element
                    classes[type]?.first?.forEach { subclassListModel.addElement(it) }
                    classes[type]?.second?.forEach { instanceListModel.addElement(it.name) }
                    instanceListModel.addElement("Unknown")
                    instanceListModel.addElement("Null")
                }
            }
            if (element is IClass) {
                AstahAccessor.getAllClasses().filter { clazz ->
                    AstahAccessor.getAttributes(clazz).any { it.initialValue == element.name } }.forEach {
                    if (!occurrenceListModel.contains(it))
                        occurrenceListModel.addElement(it)
                }
            }
        }
    }

    override fun entitySelectionChanged(p0: IEntitySelectionEvent?) {
        initialize()
        updateSelections()
    }
}