package com.udp.infodemo;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MSFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MSFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MSFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static String INDEX = "m_index";

    public MSFragment() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param index Parameter 1.
     * @return A new instance of fragment MSFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MSFragment newInstance(int index) {
        MSFragment fragment = new MSFragment();
        Bundle args = new Bundle();
        args.putInt(INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String ItemIsNull = "ItemNull";
        String ItemImage = "ItemImage";
        String ItemText = "ItemText";
        String ItemTitle = "ItemTitle";
        String ItemSubtitle = "ItemSubtitle";
        String ItemDietLevel = "ItemDietLevel";
        String ItemDate = "ItemDate";
        String ItemNurse = "ItemNurse";
        String ItemYb = "ItemYb";

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_m, container, false);
        GridView gridView = view.findViewById(R.id.gridview);
        MainActivityDemo mainActivityDemo = (MainActivityDemo)getActivity();
        if (getArguments() != null && mainActivityDemo != null) {

            int index = getArguments().getInt(INDEX);
            ArrayList<MPatient> mPatients = mainActivityDemo.pubPatients.get(index);
            if (mPatients.size() > 0) {

                ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<>();
                for(int i=0;i<mPatients.size();i++)
                {
                    MPatient mPatient = mPatients.get(i);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(ItemIsNull, TextUtils.equals(String.valueOf(mPatient.getPatname()),"无") ? "true":"false");//添加图像资源的ID
                    map.put(ItemImage, mPatient.getNursecolor());//添加图像资源的ID
                    map.put(ItemText, ""+String.valueOf(mPatient.getPatname()));//按序号做ItemText
                    map.put(ItemTitle,  ""+mPatient.getBedno()+"床");
                    map.put(ItemSubtitle, ""+mPatient.getPatisex()+" "+mPatient.getPatiage());
                    map.put(ItemDietLevel, ""+mPatient.getDietlevel());
                    map.put(ItemDate, ""+mPatient.getEnterdate());
                    map.put(ItemNurse, ""+mPatient.getNurselevel());
                    map.put(ItemYb,mPatient.getInsurancetype());
                    lstImageItem.add(map);
                }

                GridViewAdapter saImageItems = new GridViewAdapter(this.getContext(),
                        lstImageItem,//数据来源
                        R.layout.master_gridview_item,//night_item的XML`实现

                        //动态数组与ImageItem对应的子项
                        new String[] {ItemIsNull,ItemImage,ItemText,ItemTitle,ItemSubtitle,ItemDietLevel,ItemDate,ItemNurse,ItemYb},

                        new int[] {R.id.null_bed_tv,R.id.ItemImage,R.id.ItemText, R.id.tv_room_bed, R.id.tv_sub_title, R.id.tv_p_name, R.id.tv_content, R.id.tv_n_lv,R.id.tv_bq_lv});
                //添加并且显示
                gridView.setAdapter(saImageItems);

            }

        }

        return view;
    }

    private class GridViewAdapter extends SimpleAdapter {

        private void viewIsGONE(View view) {
            view.setVisibility(View.GONE);
        }

        private void viewIsVISIBLE(View view) {
            view.setVisibility(View.VISIBLE);
        }

        @Override
        public void setViewText(TextView v, String text) {

            if (TextUtils.equals("true", text) && v.getId() == R.id.null_bed_tv) {
                viewIsVISIBLE(v);
                return;
            }
            else if (v.getId() != R.id.null_bed_tv && (TextUtils.equals("无", text) || TextUtils.equals("null", text) || TextUtils.equals("无 无", text))) {
                viewIsGONE(v);
            }
            else if (R.id.null_bed_tv == v.getId()){
                viewIsGONE(v);
            }

            if (v.getId() == R.id.ItemImage) {
                if (text.contains("#"))
                    v.setBackgroundColor(Color.parseColor(text));
                return;
            }
            super.setViewText(v, text);
        }

        /**
         * Constructor
         *
         * @param context  The context where the View associated with this SimpleAdapter is running
         * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
         *                 Maps contain the data for each row, and should include all the entries specified in
         *                 "from"
         * @param resource Resource identifier of a view layout that defines the views for this list
         *                 item. The layout file should include at least those named views defined in "to"
         * @param from     A list of column names that will be added to the Map associated with each
         *                 item.
         * @param to       The views that should display column in the "from" parameter. These should all be
         *                 TextViews. The first N views in this list are given the values of the first N columns
         */
        private GridViewAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    private interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
    }
}
