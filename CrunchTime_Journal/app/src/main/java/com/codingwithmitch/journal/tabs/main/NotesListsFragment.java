package com.codingwithmitch.journal.tabs.main;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Date;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.codingwithmitch.journal.NoteEditActivity;
import com.codingwithmitch.journal.R;
import com.codingwithmitch.journal.models.Note;
import com.codingwithmitch.journal.database.NoteRepository;
import com.codingwithmitch.journal.util.ColumnSpacingItemDecorator;
import com.codingwithmitch.journal.util.VerticalSpacingItemDecorator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;


public class NotesListsFragment extends Fragment {

    public interface OnNoteListener {
        void onNoteClick(int position, ArrayList<Note> mNotes);
    }

    private static final int VERTICAL_SPACING = 20;
    private static final int COLUMN_SPACING = 10;

    private OnNoteListener mOnNoteListener;
    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;

    private ArrayList<Note> mNotes = new ArrayList<>();
    private NotesRecyclerAdapter mNoteRecyclerAdapter;
    private NoteRepository mNoteRepository;
    private Snackbar snackbar;

    public NotesListsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * pre: layout for fragment and list items exists
     * post:
     *  - RecyclerView and fragment UI initialized
     *  - if database is not empty, list of notes from database is retrieved and shown in RecyclerView
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container ViewGroup
     * @param savedInstanceState Bundle
     * @return the View for the fragment's UI, or null
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes_list, container, false);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NoteEditActivity.class);
                startActivity(intent);
            }
        });

        /* RECYCLERVIEW INITIALIZATION */

        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        //Use a 2-column layout if current display width is >= 600dp, else use a 1-column layout
        if(dpWidth >= 600){
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
            mRecyclerView.setLayoutManager(gridLayoutManager);
            ColumnSpacingItemDecorator itemDecorator = new ColumnSpacingItemDecorator(COLUMN_SPACING, VERTICAL_SPACING);
            mRecyclerView.addItemDecoration(itemDecorator);
        }
        else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(linearLayoutManager);
            VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(VERTICAL_SPACING);
            mRecyclerView.addItemDecoration(itemDecorator);
        }

        //Touch callback for list items
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);

        //Set adapter
        mNoteRecyclerAdapter = new NotesRecyclerAdapter(mNotes);
        mRecyclerView.setAdapter(mNoteRecyclerAdapter);

        /*-----------------------------*/

        //Load in notes from the database
        mNoteRepository = new NoteRepository(getActivity());
        retrieveNotes();

        snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG);
        return view;
    }

    /**
     * Helper function to get all notes from the database by timestamp (descending).
     *
     * pre: RecyclerView exists
     * post: if database isn't empty, RecyclerView reflects what is currently in the database
     */
    private void retrieveNotes() {
        mNoteRepository.retrieveNotesTask().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(@Nullable List<Note> notes) {
                if (mNotes.size() > 0) {
                    mNotes.clear();
                }
                if (notes != null) {
                    mNotes.addAll(notes);
                }
                mNoteRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Helper function to delete a note.
     *
     * pre:
     *  - a list item was swiped off of the screen
     *  - note exists in the database and in the RecyclerView
     * post:
     *  - note is deleted from RecyclerView
     *  - note is deleted from database
     *  - snackbar appears as feedback to what you just deleted
     *
     * @param note note to be deleted
     * @param pos the position of the note in the RecyclerView
     */
    private void deleteNote(Note note, final int pos) {
        final Note deletedNote = note;
        mNotes.remove(note);
        mNoteRecyclerAdapter.notifyDataSetChanged();

        mNoteRepository.deleteNoteTask(note);

        snackbar.setText("Deleted " + "\"" + note.getTitle() + "\"")
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Re-add the note that was just deleted into the view and the database
                        mNotes.add(pos, deletedNote);
                        mNoteRecyclerAdapter.notifyDataSetChanged();
                        mNoteRepository.insertNoteTask(deletedNote);
                    }
                }).show();
    }

    //Callback to delete a note after it is swiped off the screen
    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            deleteNote(mNotes.get(position), position);
        }
    };

    /**
     * Throw an exception if the fragment's activity does not implement OnNoteListener
     *
     * @param context activity content
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.d("OnAttach: ", (context instanceof OnNoteListener) + "");
        if (context instanceof OnNoteListener) {
            mOnNoteListener = (OnNoteListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNoteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnNoteListener = null;
    }

    /* VIEWHOLDER */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timestamp, title, content;

        public ViewHolder(View itemView) {
            super(itemView);
            timestamp = itemView.findViewById(R.id.note_timestamp);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);

            //make the items clickable
            itemView.setOnClickListener(this);
        }

        //defines what will happen if a list item is clicked
        @Override
        public void onClick(View view) {
            mOnNoteListener.onNoteClick(getAdapterPosition(), mNotes);
        }
    }

    /* ADAPTER */
    public class NotesRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {
        private static final String TAG = "NotesRecyclerAdapter";
        private ArrayList<Note> mNotes;

        public NotesRecyclerAdapter(ArrayList<Note> mNotes) {
            this.mNotes = mNotes;
        }

        //Set the layout for list items
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                //Convert time in milliseconds into a date
                Long epoch = Long.parseLong(mNotes.get(position).getTimestamp());
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, YYYY\nh:mm a");
                dateFormat.setTimeZone(TimeZone.getDefault());
                String timestamp = dateFormat.format(new Date(epoch));

                //Set the text shown in each list item
                holder.timestamp.setText(timestamp);
                holder.title.setText(mNotes.get(position).getTitle());
                holder.content.setText(mNotes.get(position).getContent());
            } catch (NullPointerException e) {
                Log.e(TAG, "onBindViewHolder: Null Pointer: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return mNotes.size();
        }
    }
}
