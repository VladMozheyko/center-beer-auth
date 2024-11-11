//package fr.mossaab.security.entities;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.List;
//
//@Entity
//@Table(name = "document_title")
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//public class DocumentTitle {
//    @Id
//    @GeneratedValue
//    private int id;
//
//    private String title;
//    private String ruTitle;
//
//
//    @ManyToOne
//    @JoinColumn(name = "CATEGORY_ID")
//    @JsonBackReference
//    private DocumentCategory category;
//
//    @OneToMany(mappedBy = "documentTitle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference
//    private List<FileData> files;
//}
