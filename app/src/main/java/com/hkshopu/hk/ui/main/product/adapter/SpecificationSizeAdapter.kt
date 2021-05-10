package com.hkshopu.hk.ui.main.product.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.hkshopu.hk.R
import com.hkshopu.hk.data.bean.ItemSpecification
import com.hkshopu.hk.ui.main.store.adapter.ITHelperInterface
import org.jetbrains.anko.singleLine
import java.util.*

class SpecificationSizeAdapter: RecyclerView.Adapter<SpecificationSizeAdapter.mViewHolder>(),
    ITHelperInterface {

    var unAssignList = mutableListOf<ItemSpecification>()
    lateinit var customSpecName: String
    var nextStepBtnStatus  = false

    var check_empty: Boolean  = true

    //資料變數宣告
    var value_spec : String = ""

    inner class mViewHolder(itemView: View):RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        //把layout檔的元件們拉進來，指派給當地變數
        val image = itemView.findViewById<ImageView>(R.id.btn_cancel_specification)
        val editTextView = itemView.findViewById<EditText>(R.id.edt_specification_text)

        init {

            image.setOnClickListener(this)


            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
                override fun afterTextChanged(s: Editable?) {
                    if(s.toString()==""){
                        check_empty = true
                    }else{
                        check_empty = false
                    }
                }
            }
            editTextView.addTextChangedListener(textWatcher)

            //editTextView編輯鍵盤監控
            editTextView.singleLine = true
            editTextView.setOnEditorActionListener() { v, actionId, event ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {

                        value_spec = editTextView.text.toString()

                        onItemUpdate(value_spec , adapterPosition)

                        editTextView.clearFocus()

                        true
                    }
                    else -> false
                }
            }

        }


        fun bind(item: ItemSpecification){
            //綁定當地變數與dataModel中的每個值
            image.setImageResource(item.spec_image)
            editTextView.setText(item.spec_name)

        }

        override fun onClick(v: View?) {
            when(v?.id) {
                R.id.btn_cancel_specification ->{
                    onItemDissmiss(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent:ViewGroup,viewType: Int): mViewHolder {

        //載入項目模板
        val inflater = LayoutInflater.from(parent.context)
        val example = inflater.inflate(R.layout.specification_list_item, parent, false)
        return mViewHolder(example)

    }

    override fun getItemCount() = unAssignList.size

    override fun onBindViewHolder(holder: mViewHolder, position: Int) {

        //呼叫上面的bind方法來綁定資料
        holder.bind(unAssignList[position])

    }

    //更新資料用
    fun updateList(list01:MutableList<ItemSpecification>) {
        unAssignList = list01
    }
    override fun onItemDissmiss(position: Int) {
        unAssignList.removeAt(position)
        notifyItemRemoved(position)

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(unAssignList,fromPosition,toPosition)
        notifyItemMoved(fromPosition,toPosition)
    }



    fun onItemUpdate(update_txt: String, position: Int) {
        unAssignList[position] = ItemSpecification(
            update_txt,
            R.drawable.custom_unit_transparent
        )
        notifyItemChanged(position)
    }


    fun  nextStepEnableOrNot(): Boolean {

        if(unAssignList.size > 0 ) {
            nextStepBtnStatus = true
        }else{
            nextStepBtnStatus = false
        }
        return nextStepBtnStatus

    }


    fun get_size_list(): MutableList<ItemSpecification> {
        return unAssignList
    }

    fun get_datas_size_size(): Int {
        return unAssignList.size
    }

    fun get_check_empty(): Boolean {
        return check_empty
    }
}